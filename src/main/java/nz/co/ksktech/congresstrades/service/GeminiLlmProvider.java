package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.GeminiClient;
import nz.co.ksktech.congresstrades.client.dto.GeminiGenerateRequest;
import nz.co.ksktech.congresstrades.client.dto.GeminiGenerateResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * {@link LlmProvider} backed by the Google Gemini {@code generateContent} API.
 * The system prompt is sent as Gemini's {@code systemInstruction}.
 */
@ApplicationScoped
public class GeminiLlmProvider implements LlmProvider {

    private static final Logger LOG = Logger.getLogger(GeminiLlmProvider.class);

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
        String model = appConfig.gemini().model();
        GeminiGenerateRequest request = GeminiGenerateRequest.of(
                systemPrompt, userPrompt,
                appConfig.gemini().maxTokens(), appConfig.gemini().thinkingBudget());

        LOG.infof("LLM REQUEST  → gemini POST /v1beta/models/%s:generateContent "
                        + "(systemChars=%d, userChars=%d, maxTokens=%d, thinkingBudget=%d)",
                model, systemPrompt.length(), userPrompt.length(),
                appConfig.gemini().maxTokens(), appConfig.gemini().thinkingBudget());
        LOG.debugf("LLM REQUEST body (gemini) system:%n%s%n--- user ---%n%s", systemPrompt, userPrompt);

        long start = System.currentTimeMillis();
        GeminiGenerateResponse response = geminiClient.generateContent(
                model, appConfig.gemini().apiKey().orElse(""), request);
        long ms = System.currentTimeMillis() - start;

        String text = response.firstText();
        GeminiGenerateResponse.UsageMetadata usage = response.usageMetadata();
        LOG.infof("LLM RESPONSE ← gemini (%dms, finishReason=%s, chars=%d, tokens prompt/out/thoughts=%s/%s/%s)",
                ms, response.finishReason(), text.length(),
                usage != null ? usage.promptTokenCount() : "?",
                usage != null ? usage.candidatesTokenCount() : "?",
                usage != null ? usage.thoughtsTokenCount() : "?");
        LOG.debugf("LLM RESPONSE body (gemini):%n%s", text);
        return text;
    }
}
