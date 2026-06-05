package nz.co.ksktech.congresstrades.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.client.dto.GeminiGenerateRequest;
import nz.co.ksktech.congresstrades.client.dto.GeminiGenerateResponse;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

/**
 * Typed client for the Google Gemini {@code generateContent} API.
 *
 * <p>Like the other external clients, the API key is passed per-request (here as
 * the {@code X-goog-api-key} header) and the method carries the fault-tolerance
 * ring. Used <strong>only to narrate</strong> already-computed data — never to
 * pick stocks or predict prices.</p>
 *
 * <p>Base URL is configured via {@code quarkus.rest-client.gemini-api.url}.</p>
 */
@RegisterRestClient(configKey = "gemini-api")
@RegisterProvider(ExternalClientLoggingFilter.class)
@Path("/v1beta")
public interface GeminiClient {

    @POST
    @Path("/models/{model}:generateContent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75, delay = 15000)
    GeminiGenerateResponse generateContent(@PathParam("model") String model,
                                           @HeaderParam("X-goog-api-key") String apiKey,
                                           GeminiGenerateRequest request);
}
