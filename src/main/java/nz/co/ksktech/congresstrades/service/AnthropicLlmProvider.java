package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.AnthropicClient;
import nz.co.ksktech.congresstrades.client.dto.AnthropicMessageRequest;
import nz.co.ksktech.congresstrades.client.dto.AnthropicMessageResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/**
 * {@link LlmProvider} backed by the Anthropic Messages API.
 */
@ApplicationScoped
public class AnthropicLlmProvider implements LlmProvider {

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
        AnthropicMessageRequest request = new AnthropicMessageRequest(
                appConfig.anthropic().model(),
                appConfig.anthropic().maxTokens(),
                systemPrompt,
                List.of(AnthropicMessageRequest.Message.user(userPrompt)));
        AnthropicMessageResponse response = anthropicClient.createMessage(request);
        return response.firstText();
    }
}
