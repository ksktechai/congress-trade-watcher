package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.AnthropicClient;
import nz.co.ksktech.congresstrades.client.dto.AnthropicMessageRequest;
import nz.co.ksktech.congresstrades.client.dto.AnthropicMessageResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * {@link LlmProvider} backed by the Anthropic Messages API.
 */
@ApplicationScoped
public class AnthropicLlmProvider implements LlmProvider {

    private static final Logger LOG = Logger.getLogger(AnthropicLlmProvider.class);

    @Inject
    @RestClient
    AnthropicClient anthropicClient;

    @Inject
    AppConfig appConfig;

    @Override
    public String id() {
        return "anthropic";
    }

    @Override
    public boolean isConfigured() {
        return appConfig.anthropic().apiKey().filter(k -> !k.isBlank()).isPresent();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String model = appConfig.anthropic().model();
        AnthropicMessageRequest request = new AnthropicMessageRequest(
                model,
                appConfig.anthropic().maxTokens(),
                systemPrompt,
                List.of(AnthropicMessageRequest.Message.user(userPrompt)));

        LOG.infof("LLM REQUEST  → anthropic POST /v1/messages "
                        + "(model=%s, systemChars=%d, userChars=%d, maxTokens=%d)",
                model, systemPrompt.length(), userPrompt.length(), appConfig.anthropic().maxTokens());
        LOG.debugf("LLM REQUEST body (anthropic) system:%n%s%n--- user ---%n%s", systemPrompt, userPrompt);

        long start = System.currentTimeMillis();
        AnthropicMessageResponse response = anthropicClient.createMessage(request);
        long ms = System.currentTimeMillis() - start;

        String text = response.firstText();
        AnthropicMessageResponse.Usage usage = response.usage();
        LOG.infof("LLM RESPONSE ← anthropic (%dms, stopReason=%s, chars=%d, tokens in/out=%s/%s)",
                ms, response.stopReason(), text.length(),
                usage != null ? usage.inputTokens() : "?",
                usage != null ? usage.outputTokens() : "?");
        LOG.debugf("LLM RESPONSE body (anthropic):%n%s", text);
        return text;
    }
}
