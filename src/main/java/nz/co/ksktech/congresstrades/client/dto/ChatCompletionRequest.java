package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for the OpenAI-compatible {@code POST /chat/completions} endpoint,
 * shared by every OpenAI-shaped provider (OpenRouter, local Ollama, …).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") Integer maxTokens,
        Reasoning reasoning
) {
    public record Message(String role, String content) {
        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }
    }

    public record Reasoning(boolean enabled) {
    }
}
