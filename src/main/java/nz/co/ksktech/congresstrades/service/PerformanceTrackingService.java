package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.FinnhubClient;
import nz.co.ksktech.congresstrades.client.dto.QuoteResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;

/**
 * Provides current price context for a ticker.
 *
 * <p><strong>Honest limitation:</strong> the Finnhub free tier exposes only a
 * current quote, not historical daily closes, so a true "return since the
 * disclosed trade date" cannot be computed here. This service therefore reports
 * the current quote and the day's move as a baseline, clearly labelled, rather
 * than fabricating a post-trade return. It exists to enrich research output, not
 * to score "performance" of a member's picks.</p>
 */
@ApplicationScoped
public class PerformanceTrackingService {

    private static final Logger LOG = Logger.getLogger(PerformanceTrackingService.class);

    @Inject
    @RestClient
    FinnhubClient finnhubClient;

    @Inject
    AppConfig appConfig;

    /**
     * Current price snapshot for a ticker, or {@code null} if the quote could not
     * be retrieved (caller decides how to degrade).
     */
    public PriceSnapshot snapshot(String ticker) {
        try {
            QuoteResponse quote = finnhubClient.getQuote(ticker.toUpperCase(), appConfig.finnhub().apiKey().orElse(""));
            if (quote == null || quote.current() == null) {
                return null;
            }
            return new PriceSnapshot(
                    ticker.toUpperCase(),
                    quote.current(),
                    quote.previousClose(),
                    quote.percentChange(),
                    "Current quote only; free-tier data does not include the price on the "
                            + "trade date, so no post-trade return is implied.");
        } catch (Exception e) {
            LOG.debugf(e, "Quote lookup failed for %s", ticker);
            return null;
        }
    }

    /**
     * A point-in-time price reading with an explicit caveat.
     */
    public record PriceSnapshot(
            String ticker,
            BigDecimal currentPrice,
            BigDecimal previousClose,
            BigDecimal dayChangePercent,
            String caveat
    ) {
    }
}
