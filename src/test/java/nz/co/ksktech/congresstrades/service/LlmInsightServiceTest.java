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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the prompt is built from computed data and the Anthropic response is
 * parsed correctly. The real API is never called — WireMock stands in for it.
 */
@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class LlmInsightServiceTest {

    @Inject
    LlmInsightService llmInsightService;

    @Test
    void buildUserPrompt_includesComputedTradesAndSignals() {
        String prompt = llmInsightService.buildUserPrompt(
                LocalDate.of(2026, 6, 4), sampleTrades(), sampleSignals());

        assertTrue(prompt.contains("AAPL"), "prompt should mention the ticker");
        assertTrue(prompt.contains("Alice Member"), "prompt should mention the member");
        assertTrue(prompt.contains("CLUSTER"), "prompt should mention the detected signal type");
    }

    @Test
    void generateNarrative_callsAnthropicAndParsesResponse() {
        String narrative = llmInsightService.generateNarrative(
                LocalDate.of(2026, 6, 4), sampleTrades(), sampleSignals());

        assertTrue(narrative.startsWith("MOCK DIGEST"), "should return the parsed model text");
        assertTrue(narrative.contains("not financial advice"));

        // The request the model received must carry our computed data and system rules.
        WireMockTestResource.server().verify(
                postRequestedFor(urlPathEqualTo("/v1/messages"))
                        .withRequestBody(containing("AAPL"))
                        .withRequestBody(containing("claude-sonnet-4-5")));
    }

    @Test
    void systemPrompt_forbidsRecommendations() {
        assertTrue(LlmInsightService.SYSTEM_PROMPT.contains("Do NOT give buy, sell, or hold recommendations"));
        assertEquals(true, LlmInsightService.SYSTEM_PROMPT.contains("not financial advice"));
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
