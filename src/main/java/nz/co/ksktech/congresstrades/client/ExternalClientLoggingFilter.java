package nz.co.ksktech.congresstrades.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import nz.co.ksktech.congresstrades.logging.LogSupport;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * Logs every outbound call made by the {@code @RegisterRestClient} clients (Finnhub, Gemini,
 * Anthropic, congress-data) as a request/response pair, with the same correlation id as the inbound
 * request (so all lines group together) and with credentials masked.
 *
 * <ul>
 *   <li>Request: {@code ==> METHOD masked-url} (token/api-key query params → ***) plus a capped
 *       body preview. The correlation id is also forwarded as an {@code X-Correlation-ID} header.
 *   <li>Response: {@code <== status (Nms) masked-url} plus a capped body preview.
 * </ul>
 *
 * <p>Auth headers (x-api-key, X-goog-api-key, Authorization) are never logged.
 */
@Provider
public class ExternalClientLoggingFilter implements ClientRequestFilter, ClientResponseFilter {

  private static final Logger LOG = Logger.getLogger("nz.co.ksktech.congresstrades.ext");
  private static final String START_NANOS = "extLog.startNanos";

  @Inject ObjectMapper objectMapper;

  @Override
  public void filter(ClientRequestContext request) {
    request.setProperty(START_NANOS, System.nanoTime());

    Object correlationId = MDC.get(LogSupport.MDC_CORRELATION_ID);
    if (correlationId != null && !request.getHeaders().containsKey(LogSupport.CORRELATION_HEADER)) {
      request.getHeaders().add(LogSupport.CORRELATION_HEADER, correlationId);
    }
    LOG.infof("==> %s %s", request.getMethod(), LogSupport.maskUrl(request.getUri().toString()));

    if (request.hasEntity()) {
      String body = LogSupport.preview(objectMapper, request.getEntity());
      if (!body.isEmpty()) {
        LOG.infof("==> request body: %s", body);
      }
    }
  }

  @Override
  public void filter(ClientRequestContext request, ClientResponseContext response)
      throws IOException {
    Object start = request.getProperty(START_NANOS);
    long ms = (start instanceof Long s) ? (System.nanoTime() - s) / 1_000_000 : -1;
    String url = LogSupport.maskUrl(request.getUri().toString());

    LOG.infof("<== %d %s %s (%dms)", response.getStatus(), request.getMethod(), url, ms);

    String body = readBodyPreview(response);
    if (!body.isEmpty()) {
      LOG.infof("<== response body: %s", body);
    }
  }

  /**
   * Reads up to the configured cap from the response stream for logging, then restores the full
   * stream (buffered head + untouched tail) so the client can still deserialise the entity.
   */
  private String readBodyPreview(ClientResponseContext response) throws IOException {
    int cap = LogSupport.payloadMaxChars();
    if (cap <= 0 || !response.hasEntity()) {
      return "";
    }
    InputStream stream = response.getEntityStream();
    byte[] head = stream.readNBytes(cap);
    response.setEntityStream(new SequenceInputStream(new ByteArrayInputStream(head), stream));

    String preview = new String(head, StandardCharsets.UTF_8);
    return head.length >= cap ? preview + " …(truncated)" : preview;
  }
}
