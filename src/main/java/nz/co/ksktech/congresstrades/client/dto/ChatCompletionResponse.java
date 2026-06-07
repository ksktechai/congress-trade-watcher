package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from an OpenAI-compatible {@code chat/completions} endpoint
 * (OpenRouter, local Ollama, …). The text is at
 * {@code choices[0].message.content}; extra fields (reasoning, provider, cost,
 * token details, …) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        String id,
        String model,
        String provider,
        List<Choice> choices,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(int index, Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {
    }

    public String firstText() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        Choice first = choices.get(0);
        if (first == null || first.message() == null || first.message().content() == null) {
            return "";
        }
        return first.message().content();
    }

    public String finishReason() {
        return (choices == null || choices.isEmpty() || choices.get(0) == null)
                ? null : choices.get(0).finishReason();
    }
}
