package nz.co.ksktech.congresstrades.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.client.CongressDataClient;
import nz.co.ksktech.congresstrades.client.dto.CongressTickerTrades;
import nz.co.ksktech.congresstrades.client.dto.CongressTrade;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;
import nz.co.ksktech.congresstrades.repository.WatchlistRepository;
import nz.co.ksktech.congresstrades.service.TradeUpsertService.NormalizedTrade;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Ingests real STOCK Act disclosures from the free Congress Trading Monitor
 * dataset (congress.kadoa.com) — no API key required. For each active watchlist
 * ticker it fetches that ticker's filings and upserts them idempotently using the
 * filing's stable {@code id}.
 *
 * <p>Each ticker is ingested in its own transaction so one failing symbol does
 * not roll back the rest of the run.</p>
 */
@ApplicationScoped
public class CongressDataIngestionService {

    private static final Logger LOG = Logger.getLogger(CongressDataIngestionService.class);

    @Inject
    @RestClient
    CongressDataClient congressDataClient;

    @Inject
    WatchlistRepository watchlistRepository;

    @Inject
    TradeUpsertService upsertService;

    public IngestionResult ingestWatchlist() {
        List<String> tickers = QuarkusTransaction.requiringNew().call(watchlistRepository::activeTickers);
        IngestionResult result = new IngestionResult();
        for (String ticker : tickers) {
            try {
                ingestTicker(ticker, result);
                result.tickersProcessed++;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to ingest congress data for %s", ticker);
                result.errors.add(ticker + ": " + e.getMessage());
            }
        }
        LOG.infof("Congress-data ingestion complete: %s", result);
        return result;
    }

    public IngestionResult ingestTicker(String ticker) {
        IngestionResult result = new IngestionResult();
        ingestTicker(ticker, result);
        result.tickersProcessed++;
        return result;
    }

    private void ingestTicker(String ticker, IngestionResult result) {
        String symbol = ticker.toUpperCase();
        CongressTickerTrades response = congressDataClient.getTickerTrades(symbol);
        List<CongressTrade> trades = response.trades();
        result.tradesFetched += trades.size();

        QuarkusTransaction.requiringNew().run(() -> {
            for (CongressTrade raw : trades) {
                upsertService.upsert(toNormalized(symbol, raw), result);
            }
        });
    }

    private NormalizedTrade toNormalized(String ticker, CongressTrade raw) {
        String disclosure = raw.filingDate() != null ? raw.filingDate() : raw.notificationDate();
        return new NormalizedTrade(
                raw.filerName(),
                IngestionSupport.chamberOf(raw.chamber()),
                raw.party(),
                raw.state(),
                raw.ticker() != null ? raw.ticker() : ticker,
                raw.assetName(),
                TransactionType.fromRaw(raw.transactionType()),
                raw.amountRangeLow(),
                raw.amountRangeHigh(),
                IngestionSupport.parseIsoDate(raw.transactionDate()),
                IngestionSupport.parseIsoDate(disclosure),
                raw.id());
    }
}
