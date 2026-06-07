package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.OllamaClient;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionRequest;
import nz.co.ksktech.congresstrades.client.dto.ChatCompletionResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * {@link LlmProvider} backed by a local Ollama server (OpenAI-compatible). Needs
 * no API key, so it is always "configured"; if Ollama is not running the call
 * fails with a clear connection error surfaced as the digest's 503.
 */
@ApplicationScoped
public class OllamaLlmProvider implements LlmProvider {

    private static final Logger LOG = Logger.getLogger(OllamaLlmProvider.class);

    @Inject
    @RestClient
    OllamaClient ollamaClient;

    @Inject
    AppConfig appConfig;

    @Override
    public String id() {
        return "ollama";
    }

    @Override
    public boolean isConfigured() {
        // Local, no key. Selecting this provider is itself the opt-in.
        return true;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        AppConfig.Ollama cfg = appConfig.ollama();
        ChatCompletionRequest request = new ChatCompletionRequest(
                cfg.model(),
                List.of(ChatCompletionRequest.Message.system(systemPrompt),
                        ChatCompletionRequest.Message.user(userPrompt)),
                cfg.maxTokens(),
                null);

        ChatCompletionResponse response = ollamaClient.chatCompletions(request);

        String text = response.firstText();
        ChatCompletionResponse.Usage usage = response.usage();
        LOG.infof("LLM via ollama (model=%s, finishReason=%s, chars=%d, tokens prompt/completion=%s/%s)",
                cfg.model(), response.finishReason(), text.length(),
                usage != null ? usage.promptTokens() : "?",
                usage != null ? usage.completionTokens() : "?");
        return text;
    }
}
