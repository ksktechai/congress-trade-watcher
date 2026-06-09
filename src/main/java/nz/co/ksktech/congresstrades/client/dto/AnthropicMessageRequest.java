package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request body for Anthropic's {@code POST /v1/messages} endpoint.
 *
 * @see <a href="https://docs.anthropic.com/en/api/messages">Anthropic Messages API</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicMessageRequest(
    String model,
    @JsonProperty("max_tokens") int maxTokens,
    String system,
    List<Message> messages) {
  /** A single conversational turn. {@code role} is "user" or "assistant". */
  public record Message(String role, String content) {
    public static Message user(String content) {
      return new Message("user", content);
    }
  }
}
