package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Google Gemini's {@code generateContent} endpoint:
 * <pre>
 * {
 *   "systemInstruction": { "parts": [ { "text": "..." } ] },
 *   "contents":          [ { "parts": [ { "text": "..." } ] } ],
 *   "generationConfig":  { "maxOutputTokens": 1500 }
 * }
 * </pre>
 *
 * @see <a href="https://ai.google.dev/api/generate-content">Gemini generateContent</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiGenerateRequest(
        List<Content> contents,
        Content systemInstruction,
        GenerationConfig generationConfig
) {
    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }

    public record GenerationConfig(int maxOutputTokens) {
    }

    public static GeminiGenerateRequest of(String systemPrompt, String userPrompt, int maxOutputTokens) {
        return new GeminiGenerateRequest(
                List.of(new Content(List.of(new Part(userPrompt)))),
                new Content(List.of(new Part(systemPrompt))),
                new GenerationConfig(maxOutputTokens));
    }
}
