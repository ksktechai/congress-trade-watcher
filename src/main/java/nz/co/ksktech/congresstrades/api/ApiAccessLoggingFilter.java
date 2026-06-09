package nz.co.ksktech.congresstrades.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import nz.co.ksktech.congresstrades.logging.LogSupport;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * Inbound API logging. For each call it:
 *
 * <ul>
 *   <li>assigns a <strong>correlation id</strong> (reused from the {@code X-Correlation-ID} request
 *       header, or generated) and puts it in the MDC so it prefixes every log line of that request
 *       — and echoes it back as a response header;
 *   <li>logs the request line and the response line (status + duration);
 *   <li>logs the response body, masked and capped (see {@link LogSupport}).
 * </ul>
 *
 * <p>Management endpoints ({@code /q/*}) are skipped.
 */
@Provider
@Priority(Priorities.USER)
public class ApiAccessLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final Logger LOG = Logger.getLogger("nz.co.ksktech.congresstrades.http");
  private static final String START_NANOS = "apiLog.startNanos";

  @Inject ObjectMapper objectMapper;

  @Override
  public void filter(ContainerRequestContext request) {
    String correlationId = request.getHeaderString(LogSupport.CORRELATION_HEADER);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString().substring(0, 8);
    }
    MDC.put(LogSupport.MDC_CORRELATION_ID, correlationId);
    request.setProperty(START_NANOS, System.nanoTime());

    if (!skip(request)) {
      LOG.infof("--> %s %s", request.getMethod(), pathWithQuery(request));
    }
  }

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    try {
      if (skip(request)) {
        return;
      }
      Object correlationId = MDC.get(LogSupport.MDC_CORRELATION_ID);
      if (correlationId != null) {
        response.getHeaders().putSingle(LogSupport.CORRELATION_HEADER, correlationId);
      }

      Object start = request.getProperty(START_NANOS);
      long ms = (start instanceof Long s) ? (System.nanoTime() - s) / 1_000_000 : -1;
      LOG.infof(
          "<-- %d %s %s (%dms)",
          response.getStatus(), request.getMethod(), pathWithQuery(request), ms);

      String body = LogSupport.preview(objectMapper, response.getEntity());
      if (!body.isEmpty()) {
        LOG.infof("<-- response body: %s", body);
      }
    } finally {
      MDC.remove(LogSupport.MDC_CORRELATION_ID);
    }
  }

  private boolean skip(ContainerRequestContext request) {
    return request.getUriInfo().getPath().startsWith("q/");
  }

  private String pathWithQuery(ContainerRequestContext request) {
    var uri = request.getUriInfo().getRequestUri();
    String path = uri.getRawPath();
    return uri.getRawQuery() == null ? path : LogSupport.maskUrl(path + "?" + uri.getRawQuery());
  }
}
