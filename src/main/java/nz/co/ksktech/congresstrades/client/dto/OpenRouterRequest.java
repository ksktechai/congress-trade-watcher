package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for OpenRouter's OpenAI-compatible
 * {@code POST /api/v1/chat/completions} endpoint.
 *
 * @see <a href="https://openrouter.ai/docs/api-reference/chat-completion">OpenRouter chat completions</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterRequest(
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
