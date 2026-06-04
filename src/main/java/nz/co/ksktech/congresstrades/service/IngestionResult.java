package nz.co.ksktech.congresstrades.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable counters describing the outcome of an ingestion run, shared by every
 * ingestion source.
 */
public class IngestionResult {
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
