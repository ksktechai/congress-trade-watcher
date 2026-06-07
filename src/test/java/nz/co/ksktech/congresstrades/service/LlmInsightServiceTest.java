package nz.co.ksktech.congresstrades.service;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.domain.Member;
import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.Chamber;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;
import nz.co.ksktech.congresstrades.testsupport.WireMockTestResource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the prompt is built from computed data and the LLM response is parsed.
 * The real APIs are never called — WireMock stands in for both Gemini (the default
 * provider) and Anthropic.
 */
@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class LlmInsightServiceTest {

    @Inject
    LlmInsightService llmInsightService;

    @Inject
    AnthropicLlmProvider anthropicLlmProvider;

    @Inject
    OpenRouterLlmProvider openRouterLlmProvider;

    @Inject
    OllamaLlmProvider ollamaLlmProvider;

    @Test
    void buildUserPrompt_includesComputedTradesAndSignals() {
        String prompt = llmInsightService.buildUserPrompt(
                LocalDate.of(2026, 6, 4), sampleTrades(), sampleSignals());

        assertTrue(prompt.contains("AAPL"), "prompt should mention the ticker");
        assertTrue(prompt.contains("Alice Member"), "prompt should mention the member");
        assertTrue(prompt.contains("CLUSTER"), "prompt should mention the detected signal type");
    }

    @Test
    void generateNarrative_usesDefaultGeminiProviderAndParsesResponse() {
        String narrative = llmInsightService.generateNarrative(
                LocalDate.of(2026, 6, 4), sampleTrades(), sampleSignals());

        assertTrue(narrative.startsWith("MOCK GEMINI DIGEST"), "should return the parsed Gemini text");
        assertTrue(narrative.contains("not financial advice"));

        // The Gemini request must carry the computed data and the system instruction.
        WireMockTestResource.server().verify(
                postRequestedFor(urlPathMatching("/v1beta/models/.*:generateContent"))
                        .withRequestBody(containing("AAPL"))
                        .withRequestBody(containing("systemInstruction")));
    }

    @Test
    void anthropicProvider_callsAnthropicAndParsesResponse() {
        // Directly exercise the alternative provider regardless of the default.
        String text = anthropicLlmProvider.generate(
                LlmInsightService.SYSTEM_PROMPT,
                llmInsightService.buildUserPrompt(LocalDate.of(2026, 6, 4), sampleTrades(), sampleSignals()));

        assertTrue(text.startsWith("MOCK DIGEST"), "should return the parsed Anthropic text");
        WireMockTestResource.server().verify(
                postRequestedFor(urlPathMatching("/v1/messages"))
                        .withRequestBody(containing("AAPL"))
                        .withRequestBody(containing("claude-sonnet-4-5")));
    }

    @Test
    void openRouterProvider_callsOpenRouterAndParsesResponse() {
        String text = openRouterLlmProvider.generate(
                LlmInsightService.SYSTEM_PROMPT,
                llmInsightService.buildUserPrompt(LocalDate.of(2026, 6, 4), sampleTrades(), sampleSignals()));

        assertTrue(text.startsWith("MOCK OPENROUTER DIGEST"), "should return the parsed OpenRouter text");
        WireMockTestResource.server().verify(
                postRequestedFor(urlPathMatching("/api/v1/chat/completions"))
                        .withRequestBody(containing("AAPL"))
                        .withRequestBody(containing("openrouter/free")));
    }

    @Test
    void ollamaProvider_callsLocalOllamaAndParsesResponse() {
        String text = ollamaLlmProvider.generate(
                LlmInsightService.SYSTEM_PROMPT,
                llmInsightService.buildUserPrompt(LocalDate.of(2026, 6, 4), sampleTrades(), sampleSignals()));

        assertTrue(text.startsWith("MOCK OLLAMA DIGEST"), "should return the parsed local Ollama text");
        WireMockTestResource.server().verify(
                postRequestedFor(urlPathMatching("/v1/chat/completions"))
                        .withRequestBody(containing("AAPL"))
                        .withRequestBody(containing("qwen3:30b")));
    }

    @Test
    void systemPrompt_forbidsRecommendations() {
        assertTrue(LlmInsightService.SYSTEM_PROMPT.contains("Do NOT give buy, sell, or hold recommendations"));
        assertTrue(LlmInsightService.SYSTEM_PROMPT.contains("not financial advice"));
    }

    private List<Trade> sampleTrades() {
        Trade t = new Trade();
        t.member = new Member("Alice Member", Chamber.HOUSE, "I", "CA");
        t.ticker = "AAPL";
        t.transactionType = TransactionType.PURCHASE;
        t.amountRangeLow = BigDecimal.valueOf(1001);
        t.amountRangeHigh = BigDecimal.valueOf(15000);
        t.transactionDate = LocalDate.of(2026, 5, 1);
        t.disclosureDate = LocalDate.of(2026, 5, 10);
        t.recomputeDaysToDisclose();
        return List.of(t);
    }

    private List<Signal> sampleSignals() {
        return List.of(new Signal(SignalType.CLUSTER, "AAPL",
                "3 distinct members purchased AAPL within a 14-day window.", 3.0, List.of()));
    }
}
