package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import nz.co.ksktech.congresstrades.client.OpenRouterClient;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionRequest;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * {@link LlmProvider} backed by OpenRouter (OpenAI-compatible chat completions). The system prompt
 * is sent as a {@code system} message and the computed data as a {@code user} message.
 */
@ApplicationScoped
public class OpenRouterLlmProvider implements LlmProvider {

  private static final Logger LOG = Logger.getLogger(OpenRouterLlmProvider.class);

  @Inject @RestClient OpenRouterClient openRouterClient;

  @Inject AppConfig appConfig;

  @Override
  public String id() {
    return "openrouter";
  }

  @Override
  public boolean isConfigured() {
    return appConfig.openrouter().apiKey().filter(k -> !k.isBlank()).isPresent();
  }

  @Override
  public String generate(String systemPrompt, String userPrompt) {
    AppConfig.OpenRouter cfg = appConfig.openrouter();
    ChatCompletionRequest request =
        new ChatCompletionRequest(
            cfg.model(),
            List.of(
                ChatCompletionRequest.Message.system(systemPrompt),
                ChatCompletionRequest.Message.user(userPrompt)),
            cfg.maxTokens(),
            cfg.reasoning() ? new ChatCompletionRequest.Reasoning(true) : null);

    ChatCompletionResponse response =
        openRouterClient.chatCompletions("Bearer " + cfg.apiKey().orElse(""), request);

    String text = response.firstText();
    ChatCompletionResponse.Usage usage = response.usage();
    LOG.infof(
        "LLM via openrouter (requested=%s, served=%s/%s, finishReason=%s, chars=%d, "
            + "tokens prompt/completion=%s/%s)",
        cfg.model(),
        response.provider(),
        response.model(),
        response.finishReason(),
        text.length(),
        usage != null ? usage.promptTokens() : "?",
        usage != null ? usage.completionTokens() : "?");
    return text;
  }
}
