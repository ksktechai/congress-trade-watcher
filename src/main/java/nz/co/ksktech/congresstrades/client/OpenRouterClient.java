package nz.co.ksktech.congresstrades.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.temporal.ChronoUnit;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionRequest;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionResponse;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Typed client for the OpenRouter OpenAI-compatible chat-completions API.
 *
 * <p>The API key is passed per request as the {@code Authorization: Bearer …} header (never
 * logged). Used <strong>only to narrate</strong> already-computed data. Base URL via {@code
 * quarkus.rest-client.openrouter-api.url}.
 */
@RegisterRestClient(configKey = "openrouter-api")
@RegisterProvider(ExternalClientLoggingFilter.class)
@Path("/api/v1")
public interface OpenRouterClient {

  @POST
  @Path("/chat/completions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Timeout(value = 30, unit = ChronoUnit.SECONDS)
  @Retry(maxRetries = 2, delay = 1000)
  @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75, delay = 15000)
  ChatCompletionResponse chatCompletions(
      @HeaderParam("Authorization") String authorization, ChatCompletionRequest request);
}
