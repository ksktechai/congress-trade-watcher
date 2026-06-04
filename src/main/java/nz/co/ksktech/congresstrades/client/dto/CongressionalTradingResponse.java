package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Top-level shape of Finnhub's {@code /stock/congressional-trading} response:
 * <pre>{ "data": [ ... ], "symbol": "AAPL" }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CongressionalTradingResponse(
        List<CongressionalTrade> data,
        String symbol
) {
    public List<CongressionalTrade> data() {
        return data == null ? List.of() : data;
    }
}
