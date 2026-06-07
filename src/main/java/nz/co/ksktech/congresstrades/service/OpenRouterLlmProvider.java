package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.OpenRouterClient;
import nz.co.ksktech.congresstrades.client.dto.OpenRouterRequest;
import nz.co.ksktech.congresstrades.client.dto.OpenRouterResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * {@link LlmProvider} backed by OpenRouter (OpenAI-compatible chat completions).
 * The system prompt is sent as a {@code system} message and the computed data as
 * a {@code user} message.
 */
@ApplicationScoped
public class OpenRouterLlmProvider implements LlmProvider {

    private static final Logger LOG = Logger.getLogger(OpenRouterLlmProvider.class);

    @Inject
    @RestClient
    OpenRouterClient openRouterClient;

    @Inject
    AppConfig appConfig;

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
        OpenRouterRequest request = new OpenRouterRequest(
                cfg.model(),
                List.of(OpenRouterRequest.Message.system(systemPrompt),
                        OpenRouterRequest.Message.user(userPrompt)),
                cfg.maxTokens(),
                cfg.reasoning() ? new OpenRouterRequest.Reasoning(true) : null);

        OpenRouterResponse response = openRouterClient.chatCompletions(
                "Bearer " + cfg.apiKey().orElse(""), request);

        String text = response.firstText();
        OpenRouterResponse.Usage usage = response.usage();
        LOG.infof("LLM via openrouter (requested=%s, served=%s/%s, finishReason=%s, chars=%d, "
                        + "tokens prompt/completion=%s/%s)",
                cfg.model(), response.provider(), response.model(), response.finishReason(), text.length(),
                usage != null ? usage.promptTokens() : "?",
                usage != null ? usage.completionTokens() : "?");
        return text;
    }
}
