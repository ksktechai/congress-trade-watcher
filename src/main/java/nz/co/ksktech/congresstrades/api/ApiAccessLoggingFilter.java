package nz.co.ksktech.congresstrades.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Logs each inbound API call as a request/response pair, bracketing the service
 * work in between so the full sequence of calls is readable in the logs:
 *
 * <pre>
 * API REQUEST  → GET /api/v1/digest/daily
 * DIGEST step 1/3 ...
 * INGEST/LLM REQUEST/RESPONSE ...
 * API RESPONSE ← GET /api/v1/digest/daily : 200 (842ms)
 * </pre>
 *
 * <p>Management endpoints ({@code /q/*}: health, openapi, swagger) are skipped to
 * keep the log focused on the application API.</p>
 */
@Provider
public class ApiAccessLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger("nz.co.ksktech.congresstrades.api.access");
    private static final String START_NANOS = "apiAccess.startNanos";

    @Override
    public void filter(ContainerRequestContext request) {
        request.setProperty(START_NANOS, System.nanoTime());
        if (!skip(request)) {
            LOG.infof("API REQUEST  → %s %s", request.getMethod(), pathWithQuery(request));
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (skip(request)) {
            return;
        }
        Object start = request.getProperty(START_NANOS);
        long ms = (start instanceof Long s) ? (System.nanoTime() - s) / 1_000_000 : -1;
        LOG.infof("API RESPONSE ← %s %s : %d (%dms)",
                request.getMethod(), pathWithQuery(request), response.getStatus(), ms);
    }

    private boolean skip(ContainerRequestContext request) {
        return request.getUriInfo().getPath().startsWith("q/");
    }

    private String pathWithQuery(ContainerRequestContext request) {
        var uri = request.getUriInfo().getRequestUri();
        String path = uri.getRawPath();
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }
}
