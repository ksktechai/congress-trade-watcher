package nz.co.ksktech.congresstrades.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
        stubFinnhub();
        stubAnthropic();

        String baseUrl = wireMockServer.baseUrl();
        return Map.of(
                "quarkus.rest-client.finnhub-api.url", baseUrl,
                "quarkus.rest-client.anthropic-api.url", baseUrl);
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

    @Override
    public void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}
