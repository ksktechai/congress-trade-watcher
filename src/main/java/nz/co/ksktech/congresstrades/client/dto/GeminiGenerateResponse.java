package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response body from Gemini's {@code generateContent}. The text lives at {@code
 * candidates[0].content.parts[*].text}; {@link #firstText()} extracts it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiGenerateResponse(
    List<Candidate> candidates, @JsonProperty("usageMetadata") UsageMetadata usageMetadata) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Candidate(Content content, String finishReason) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Content(List<Part> parts, String role) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Part(String text) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UsageMetadata(
      @JsonProperty("promptTokenCount") Integer promptTokenCount,
      @JsonProperty("candidatesTokenCount") Integer candidatesTokenCount,
      @JsonProperty("thoughtsTokenCount") Integer thoughtsTokenCount,
      @JsonProperty("totalTokenCount") Integer totalTokenCount) {}

  /** Concatenated text of the first candidate's parts. */
  public String firstText() {
    if (candidates == null || candidates.isEmpty()) {
      return "";
    }
    Candidate first = candidates.get(0);
    if (first == null || first.content() == null || first.content().parts() == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Part part : first.content().parts()) {
      if (part != null && part.text() != null) {
        sb.append(part.text());
      }
    }
    return sb.toString();
  }

  /** Short {@code finishReason} of the first candidate, for logging. */
  public String finishReason() {
    return (candidates == null || candidates.isEmpty() || candidates.get(0) == null)
        ? null
        : candidates.get(0).finishReason();
  }
}
