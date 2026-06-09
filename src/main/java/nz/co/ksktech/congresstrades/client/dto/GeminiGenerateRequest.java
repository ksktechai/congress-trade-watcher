package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Request body for Google Gemini's {@code generateContent} endpoint:
 *
 * <pre>
 * {
 *   "systemInstruction": { "parts": [ { "text": "..." } ] },
 *   "contents":          [ { "parts": [ { "text": "..." } ] } ],
 *   "generationConfig":  { "maxOutputTokens": 2048, "thinkingConfig": { "thinkingBudget": 0 } }
 * }
 * </pre>
 *
 * <p>{@code thinkingBudget: 0} disables the 2.5-series "thinking" phase, which otherwise consumes
 * the output-token budget and can truncate the narrative.
 *
 * @see <a href="https://ai.google.dev/api/generate-content">Gemini generateContent</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiGenerateRequest(
    List<Content> contents, Content systemInstruction, GenerationConfig generationConfig) {
  public record Content(List<Part> parts) {}

  public record Part(String text) {}

  public record GenerationConfig(int maxOutputTokens, ThinkingConfig thinkingConfig) {}

  public record ThinkingConfig(int thinkingBudget) {}

  /**
   * @param thinkingBudget tokens the model may spend "thinking"; {@code 0} disables it, a negative
   *     value omits the field (for models that do not support thinking config).
   */
  public static GeminiGenerateRequest of(
      String systemPrompt, String userPrompt, int maxOutputTokens, int thinkingBudget) {
    ThinkingConfig thinking = thinkingBudget >= 0 ? new ThinkingConfig(thinkingBudget) : null;
    return new GeminiGenerateRequest(
        List.of(new Content(List.of(new Part(userPrompt)))),
        new Content(List.of(new Part(systemPrompt))),
        new GenerationConfig(maxOutputTokens, thinking));
  }
}
