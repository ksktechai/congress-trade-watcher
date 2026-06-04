package nz.co.ksktech.congresstrades.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.FinnhubClient;
import nz.co.ksktech.congresstrades.client.dto.CongressionalTrade;
import nz.co.ksktech.congresstrades.client.dto.CongressionalTradingResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.domain.Member;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.Chamber;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;
import nz.co.ksktech.congresstrades.repository.MemberRepository;
import nz.co.ksktech.congresstrades.repository.TradeRepository;
import nz.co.ksktech.congresstrades.repository.WatchlistRepository;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches disclosures from Finnhub for every active watchlist ticker, then
 * persists them idempotently.
 *
 * <p>Idempotency: Finnhub does not expose a stable filing id, so we derive a
 * deterministic {@code sourceFilingId} from the immutable fields of a disclosure
 * (member, ticker, dates, type, amount range). Re-running ingestion therefore
 * updates existing rows instead of duplicating them, backed by the unique
 * constraint on {@code source_filing_id}.</p>
 *
 * <p>Each ticker is ingested in its own transaction so one failing symbol does
 * not roll back the rest of the run.</p>
 */
@ApplicationScoped
public class TradeIngestionService {

    private static final Logger LOG = Logger.getLogger(TradeIngestionService.class);

    @Inject
    @RestClient
    FinnhubClient finnhubClient;

    @Inject
    AppConfig appConfig;

    @Inject
    WatchlistRepository watchlistRepository;

    @Inject
    MemberRepository memberRepository;

    @Inject
    TradeRepository tradeRepository;

    /**
     * Ingest disclosures for every active watchlist ticker.
     */
    public IngestionResult ingestWatchlist() {
        List<String> tickers = QuarkusTransaction.requiringNew().call(watchlistRepository::activeTickers);
        IngestionResult result = new IngestionResult();
        for (String ticker : tickers) {
            try {
                ingestTickerInNewTransaction(ticker, result);
                result.tickersProcessed++;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to ingest ticker %s", ticker);
                result.errors.add(ticker + ": " + e.getMessage());
            }
        }
        LOG.infof("Ingestion complete: %s", result);
        return result;
    }

    /**
     * Ingest a single ticker (also used by the admin trigger for ad-hoc symbols).
     */
    public IngestionResult ingestTicker(String ticker) {
        IngestionResult result = new IngestionResult();
        ingestTickerInNewTransaction(ticker, result);
        result.tickersProcessed++;
        return result;
    }

    private void ingestTickerInNewTransaction(String ticker, IngestionResult result) {
        String token = appConfig.finnhub().apiKey();
        CongressionalTradingResponse response = finnhubClient.getCongressionalTrades(ticker, token);
        List<CongressionalTrade> trades = response.data();
        result.tradesFetched += trades.size();

        QuarkusTransaction.requiringNew().run(() -> {
            for (CongressionalTrade raw : trades) {
                upsertTrade(ticker.toUpperCase(), raw, result);
            }
        });
    }

    private void upsertTrade(String ticker, CongressionalTrade raw, IngestionResult result) {
        if (raw.name() == null || raw.name().isBlank()) {
            return; // can't attribute a trade with no member
        }
        LocalDate transactionDate = parseDate(raw.transactionDate());
        LocalDate filingDate = parseDate(raw.filingDate());
        String sourceFilingId = buildFilingId(ticker, raw);

        Member member = memberRepository.findByFullName(raw.name().trim())
                .orElseGet(() -> {
                    Member m = new Member(raw.name().trim(), Chamber.UNKNOWN, null, null);
                    memberRepository.persist(m);
                    return m;
                });

        Trade trade = tradeRepository.findBySourceFilingId(sourceFilingId).orElse(null);
        boolean isNew = trade == null;
        if (isNew) {
            trade = new Trade();
            trade.sourceFilingId = sourceFilingId;
        }

        trade.member = member;
        trade.ticker = ticker;
        trade.assetDescription = raw.assetName();
        trade.transactionType = TransactionType.fromRaw(raw.transactionType());
        trade.amountRangeLow = raw.amountFrom();
        trade.amountRangeHigh = raw.amountTo();
        trade.transactionDate = transactionDate;
        trade.disclosureDate = filingDate;
        trade.recomputeDaysToDisclose();

        if (isNew) {
            tradeRepository.persist(trade);
            result.newTrades++;
        } else {
            result.updatedTrades++;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // Finnhub returns ISO dates; tolerate an optional time component.
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (DateTimeParseException e) {
            LOG.debugf("Unparseable date '%s'", value);
            return null;
        }
    }

    /**
     * Deterministic, collision-resistant id from the immutable parts of a
     * disclosure, so re-ingestion is idempotent.
     */
    private String buildFilingId(String ticker, CongressionalTrade raw) {
        String seed = String.join("|",
                safe(raw.name()), ticker, safe(raw.transactionDate()), safe(raw.filingDate()),
                safe(raw.transactionType()), safe(String.valueOf(raw.amountFrom())),
                safe(String.valueOf(raw.amountTo())));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 16; i++) { // 32 hex chars is plenty
                hex.append(String.format("%02x", digest[i]));
            }
            return ticker + "-" + hex;
        } catch (NoSuchAlgorithmException e) {
            return ticker + "-" + Integer.toHexString(seed.hashCode());
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Mutable counters describing the outcome of an ingestion run.
     */
    public static class IngestionResult {
        public int tickersProcessed;
        public int tradesFetched;
        public int newTrades;
        public int updatedTrades;
        public List<String> errors = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("tickers=%d fetched=%d new=%d updated=%d errors=%d",
                    tickersProcessed, tradesFetched, newTrades, updatedTrades, errors.size());
        }
    }
}
