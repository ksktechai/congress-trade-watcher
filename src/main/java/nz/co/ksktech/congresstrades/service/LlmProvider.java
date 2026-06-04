package nz.co.ksktech.congresstrades.service;

/**
 * Strategy for the narration LLM. Implementations wrap one provider's API and are
 * selected at runtime by {@code watcher.llm.provider}. Adding a provider is just a
 * new {@code @ApplicationScoped} implementation of this interface.
 *
 * <p>Providers narrate only — they receive a system prompt (which forbids
 * recommendations) and a user prompt built from already-computed data, and return
 * the model's text.</p>
 */
public interface LlmProvider {

    /** Stable id matched against {@code watcher.llm.provider} (case-insensitive). */
    String id();

    /** Whether this provider's API key is present, so the digest can be attempted. */
    boolean isConfigured();

    /** Call the provider and return the generated narrative text. */
    String generate(String systemPrompt, String userPrompt);
}
