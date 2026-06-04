package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Shape of {@code /data/ticker/{TICKER}.json} from the Congress Trading Monitor
 * dataset: {@code { "ticker": "AAPL", "trades": [ ... ], "price": { ... } }}.
 * The price block is ignored here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CongressTickerTrades(
        String ticker,
        List<CongressTrade> trades
) {
    public List<CongressTrade> trades() {
        return trades == null ? List.of() : trades;
    }
}
