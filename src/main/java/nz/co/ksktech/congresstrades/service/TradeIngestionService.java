package nz.co.ksktech.congresstrades.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import nz.co.ksktech.congresstrades.client.FinnhubClient;
import nz.co.ksktech.congresstrades.client.dto.CongressionalTrade;
import nz.co.ksktech.congresstrades.client.dto.CongressionalTradingResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.domain.enums.Chamber;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;
import nz.co.ksktech.congresstrades.repository.WatchlistRepository;
import nz.co.ksktech.congresstrades.service.TradeUpsertService.NormalizedTrade;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Fetches disclosures from Finnhub for every active watchlist ticker, then persists them
 * idempotently via {@link TradeUpsertService}.
 *
 * <p>Idempotency: Finnhub exposes no stable filing id, so we derive a deterministic {@code
 * sourceFilingId} from the immutable fields of a disclosure. Finnhub's congressional-trading
 * endpoint is premium, so this source may return HTTP 403 on a free key — the alternative {@link
 * CongressDataIngestionService} is free and is the default ingestion source.
 */
@ApplicationScoped
public class TradeIngestionService {

  private static final Logger LOG = Logger.getLogger(TradeIngestionService.class);

  @Inject @RestClient FinnhubClient finnhubClient;

  @Inject AppConfig appConfig;

  @Inject WatchlistRepository watchlistRepository;

  @Inject TradeUpsertService upsertService;

  /** Ingest disclosures for every active watchlist ticker. */
  public IngestionResult ingestWatchlist() {
    List<String> tickers =
        QuarkusTransaction.requiringNew().call(watchlistRepository::activeTickers);
    IngestionResult result = new IngestionResult();
    for (String ticker : tickers) {
      try {
        ingestTicker(ticker, result);
        result.tickersProcessed++;
      } catch (Exception e) {
        LOG.errorf(e, "Failed to ingest ticker %s", ticker);
        result.errors.add(ticker + ": " + e.getMessage());
      }
    }
    LOG.infof("Finnhub ingestion complete: %s", result);
    return result;
  }

  /** Ingest a single ticker (also used by the admin trigger for ad-hoc symbols). */
  public IngestionResult ingestTicker(String ticker) {
    IngestionResult result = new IngestionResult();
    ingestTicker(ticker, result);
    result.tickersProcessed++;
    return result;
  }

  private void ingestTicker(String ticker, IngestionResult result) {
    String token = appConfig.finnhub().apiKey().orElse("");
    CongressionalTradingResponse response = finnhubClient.getCongressionalTrades(ticker, token);
    List<CongressionalTrade> trades = response.data();
    result.tradesFetched += trades.size();

    String symbol = ticker.toUpperCase();
    QuarkusTransaction.requiringNew()
        .run(
            () -> {
              for (CongressionalTrade raw : trades) {
                upsertService.upsert(toNormalized(symbol, raw), result);
              }
            });
  }

  private NormalizedTrade toNormalized(String ticker, CongressionalTrade raw) {
    return new NormalizedTrade(
        raw.name(),
        Chamber.UNKNOWN, // Finnhub does not provide chamber/party/state
        null,
        null,
        ticker,
        raw.assetName(),
        TransactionType.fromRaw(raw.transactionType()),
        raw.amountFrom(),
        raw.amountTo(),
        IngestionSupport.parseIsoDate(raw.transactionDate()),
        IngestionSupport.parseIsoDate(raw.filingDate()),
        buildFilingId(ticker, raw));
  }

  /**
   * Deterministic, collision-resistant id from the immutable parts of a disclosure, so re-ingestion
   * is idempotent.
   */
  private String buildFilingId(String ticker, CongressionalTrade raw) {
    String seed =
        String.join(
            "|",
            safe(raw.name()),
            ticker,
            safe(raw.transactionDate()),
            safe(raw.filingDate()),
            safe(raw.transactionType()),
            safe(String.valueOf(raw.amountFrom())),
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
}
