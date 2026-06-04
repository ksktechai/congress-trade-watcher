package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.GeminiClient;
import nz.co.ksktech.congresstrades.client.dto.GeminiGenerateRequest;
import nz.co.ksktech.congresstrades.client.dto.GeminiGenerateResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * {@link LlmProvider} backed by the Google Gemini {@code generateContent} API.
 * The system prompt is sent as Gemini's {@code systemInstruction}.
 */
@ApplicationScoped
public class GeminiLlmProvider implements LlmProvider {

    @Inject
    @RestClient
    GeminiClient geminiClient;

    @Inject
    AppConfig appConfig;

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return appConfig.gemini().apiKey().filter(k -> !k.isBlank()).isPresent();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        GeminiGenerateRequest request = GeminiGenerateRequest.of(
                systemPrompt, userPrompt, appConfig.gemini().maxTokens());
        GeminiGenerateResponse response = geminiClient.generateContent(
                appConfig.gemini().model(),
                appConfig.gemini().apiKey().orElse(""),
                request);
        return response.firstText();
    }
}
