package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body from Anthropic's {@code POST /v1/messages}. The model's text is
 * delivered as an array of typed {@code content} blocks; {@link #firstText()}
 * extracts the concatenated text.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicMessageResponse(
        String id,
        String type,
        String role,
        String model,
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {
    }

    /**
     * Concatenates the text of all {@code text} content blocks.
     */
    public String firstText() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : content) {
            if (block != null && "text".equals(block.type()) && block.text() != null) {
                sb.append(block.text());
            }
        }
        return sb.toString();
    }
}
