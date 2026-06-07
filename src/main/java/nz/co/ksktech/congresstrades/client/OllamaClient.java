package nz.co.ksktech.congresstrades.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionRequest;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionResponse;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

/**
 * Typed client for a local <a href="https://ollama.com">Ollama</a> server's
 * OpenAI-compatible chat-completions endpoint
 * ({@code http://localhost:11434/v1/chat/completions}).
 *
 * <p>Fully local: no API key, no rate limits, nothing leaves the machine. The
 * timeout is generous because local models can be slow. Base URL via
 * {@code quarkus.rest-client.ollama-api.url}.</p>
 */
@RegisterRestClient(configKey = "ollama-api")
@RegisterProvider(ExternalClientLoggingFilter.class)
@Path("/v1")
public interface OllamaClient {

    @POST
    @Path("/chat/completions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(value = 180, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 1, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75, delay = 15000)
    ChatCompletionResponse chatCompletions(ChatCompletionRequest request);
}
