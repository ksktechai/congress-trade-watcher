package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.api.exception.LlmUnavailableException;
import nz.co.ksktech.congresstrades.client.AnthropicClient;
import nz.co.ksktech.congresstrades.client.dto.AnthropicMessageRequest;
import nz.co.ksktech.congresstrades.client.dto.AnthropicMessageResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.Trade;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Turns the app's already-computed trades and signals into a readable, plain-English
 * morning-briefing narrative by calling the Anthropic Messages API.
 *
 * <p><strong>Boundary:</strong> the model only <em>summarises and contextualises</em>
 * data this app has already derived. The system prompt forbids buy/sell
 * recommendations and price predictions and requires the data's limitations to be
 * stated. The LLM never selects tickers or produces signals — that is the job of
 * the pure-code {@link SignalDetectionService}.</p>
 */
@ApplicationScoped
public class LlmInsightService {

    private static final Logger LOG = Logger.getLogger(LlmInsightService.class);

    static final String SYSTEM_PROMPT = """
            You are a careful research assistant summarising US Congressional stock-trade
            disclosures for a daily briefing. You will be given data the application has
            already computed: a list of disclosed trades and a list of detected, rule-based
            signals.

            STRICT RULES:
            - Summarise and contextualise ONLY the data provided. Do not invent trades,
              members, tickers, prices, or signals.
            - Do NOT give buy, sell, or hold recommendations. Do NOT predict prices or
              future performance. Do NOT imply any trade is a good or bad investment.
            - Always remind the reader that congressional disclosures are delayed up to
              45 days, that amounts are broad ranges (not exact figures), and that this is
              research material, not financial advice.
            - Be concise and neutral. Prefer short paragraphs and, where helpful, bullet
              points. Group observations by ticker or by signal type.
            - If the data is sparse, say so plainly rather than padding.
            """;

    @Inject
    @RestClient
    AnthropicClient anthropicClient;

    @Inject
    AppConfig appConfig;

    /**
     * Builds the user prompt from computed data. Deterministic and side-effect
     * free, so it can be asserted in unit tests.
     */
    public String buildUserPrompt(LocalDate date, List<Trade> trades, List<Signal> signals) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date of briefing: ").append(date).append("\n\n");

        sb.append("=== DETECTED SIGNALS (rule-based, computed by the app) ===\n");
        if (signals.isEmpty()) {
            sb.append("No signals were detected in the look-back window.\n");
        } else {
            for (Signal s : signals) {
                sb.append("- [").append(s.signalType).append("] ")
                        .append(s.ticker != null ? s.ticker + ": " : "")
                        .append(s.description)
                        .append(" (score=").append(s.score).append(")\n");
            }
        }

        sb.append("\n=== DISCLOSED TRADES BY TICKER ===\n");
        if (trades.isEmpty()) {
            sb.append("No trades in the look-back window.\n");
        } else {
            Map<String, List<Trade>> byTicker = trades.stream()
                    .filter(t -> t.ticker != null)
                    .collect(Collectors.groupingBy(t -> t.ticker, TreeMap::new, Collectors.toList()));
            for (Map.Entry<String, List<Trade>> entry : byTicker.entrySet()) {
                sb.append(entry.getKey()).append(":\n");
                for (Trade t : entry.getValue()) {
                    String member = t.member != null ? t.member.fullName : "Unknown";
                    sb.append("  - ").append(member)
                            .append(" ").append(t.transactionType)
                            .append(" range $").append(t.amountRangeLow).append("-$").append(t.amountRangeHigh)
                            .append(", traded ").append(t.transactionDate)
                            .append(", disclosed ").append(t.disclosureDate)
                            .append(" (").append(t.daysToDisclose).append(" days later)\n");
                }
            }
        }

        sb.append("\nWrite a neutral morning-briefing summary of the above for a reader ")
                .append("researching congressional trading patterns. Follow all the strict rules.");
        return sb.toString();
    }

    /**
     * Calls Anthropic to produce the narrative. Throws {@link LlmUnavailableException}
     * on any failure (missing key, timeout, open circuit) so callers can degrade.
     */
    public String generateNarrative(LocalDate date, List<Trade> trades, List<Signal> signals) {
        if (appConfig.anthropic().apiKey() == null || appConfig.anthropic().apiKey().isBlank()) {
            throw new LlmUnavailableException(
                    "ANTHROPIC_API_KEY is not configured; cannot generate the digest narrative.");
        }
        String userPrompt = buildUserPrompt(date, trades, signals);
        AnthropicMessageRequest request = new AnthropicMessageRequest(
                appConfig.anthropic().model(),
                appConfig.anthropic().maxTokens(),
                SYSTEM_PROMPT,
                List.of(AnthropicMessageRequest.Message.user(userPrompt)));
        try {
            AnthropicMessageResponse response = anthropicClient.createMessage(request);
            String text = response.firstText();
            if (text == null || text.isBlank()) {
                throw new LlmUnavailableException("Anthropic returned an empty response.");
            }
            return text;
        } catch (LlmUnavailableException e) {
            throw e;
        } catch (Exception e) {
            LOG.warnf(e, "Anthropic call failed");
            throw new LlmUnavailableException("Failed to generate digest via Anthropic: " + e.getMessage(), e);
        }
    }
}
