package nz.co.ksktech.congresstrades.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Boots a WireMock server and rewires the Finnhub and Anthropic REST clients to
 * it, so the test suite NEVER touches the real external APIs.
 *
 * <p>The running server is exposed via {@link #server()} so individual tests can
 * add assertions (e.g. verify the exact request Anthropic received).</p>
 */
public class WireMockTestResource implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer wireMockServer;

    public static WireMockServer server() {
        return wireMockServer;
    }

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        stubCongressData();
        stubFinnhub();
        stubAnthropic();
        stubGemini();

        String baseUrl = wireMockServer.baseUrl();
        return Map.of(
                "quarkus.rest-client.congress-data.url", baseUrl,
                "quarkus.rest-client.finnhub-api.url", baseUrl,
                "quarkus.rest-client.anthropic-api.url", baseUrl,
                "quarkus.rest-client.gemini-api.url", baseUrl);
    }

    private void stubCongressData() {
        // Real-shaped Congress Trading Monitor records, returned for any ticker.
        // The second filing is 42 days late so a LATE_DISCLOSURE signal is produced.
        String body = """
                {
                  "ticker": "AAPL",
                  "trades": [
                    {
                      "id": "house_test_1",
                      "filer_name": "Jane Representative",
                      "chamber": "house", "party": "D", "state": "CA",
                      "ticker": "AAPL", "asset_name": "Apple Inc. - Common Stock",
                      "transaction_type": "Purchase",
                      "amount_range_low": 1001, "amount_range_high": 15000,
                      "transaction_date": "2026-05-01", "filing_date": "2026-05-10",
                      "is_late": 0
                    },
                    {
                      "id": "senate_test_1",
                      "filer_name": "John Senator",
                      "chamber": "senate", "party": "R", "state": "TX",
                      "ticker": "AAPL", "asset_name": "Apple Inc. - Common Stock",
                      "transaction_type": "Sale (Full)",
                      "amount_range_low": 250001, "amount_range_high": 500000,
                      "transaction_date": "2026-04-01", "filing_date": "2026-05-13",
                      "is_late": 1
                    }
                  ],
                  "price": {}
                }
                """;
        wireMockServer.stubFor(get(urlPathMatching("/data/ticker/.*\\.json"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private void stubFinnhub() {
        // Two disclosures, regardless of the symbol queried. The second is filed
        // 42 days late so a LATE_DISCLOSURE signal is produced.
        String congressionalBody = """
                {
                  "data": [
                    {
                      "name": "Jane Representative",
                      "amountFrom": 1001,
                      "amountTo": 15000,
                      "assetName": "Test Asset",
                      "transactionType": "Purchase",
                      "transactionDate": "2026-05-01",
                      "filingDate": "2026-05-10",
                      "ownerType": "",
                      "position": ""
                    },
                    {
                      "name": "John Senator",
                      "amountFrom": 250001,
                      "amountTo": 500000,
                      "assetName": "Test Asset",
                      "transactionType": "Sale",
                      "transactionDate": "2026-04-01",
                      "filingDate": "2026-05-13",
                      "ownerType": "",
                      "position": ""
                    }
                  ],
                  "symbol": "TEST"
                }
                """;
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/stock/congressional-trading"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(congressionalBody)));

        String quoteBody = """
                {"c": 191.24, "d": 1.50, "dp": 0.79, "h": 192.0, "l": 188.5, "o": 189.0, "pc": 189.74, "t": 1717459200}
                """;
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/quote"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(quoteBody)));
    }

    private void stubAnthropic() {
        String messageBody = """
                {
                  "id": "msg_test_123",
                  "type": "message",
                  "role": "assistant",
                  "model": "claude-sonnet-4-5",
                  "content": [
                    {"type": "text", "text": "MOCK DIGEST: Two disclosures were observed. Remember these are delayed up to 45 days and amounts are ranges. This is not financial advice."}
                  ],
                  "stop_reason": "end_turn",
                  "usage": {"input_tokens": 120, "output_tokens": 60}
                }
                """;
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(messageBody)));
    }

    private void stubGemini() {
        String generateBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "role": "model",
                        "parts": [
                          {"text": "MOCK GEMINI DIGEST: Two disclosures were observed. Remember these are delayed up to 45 days and amounts are ranges. This is not financial advice."}
                        ]
                      },
                      "finishReason": "STOP"
                    }
                  ],
                  "usageMetadata": {"promptTokenCount": 120, "candidatesTokenCount": 60, "totalTokenCount": 180}
                }
                """;
        wireMockServer.stubFor(post(urlPathMatching("/v1beta/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(generateBody)));
    }

    @Override
    public void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}
