package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Top-level shape of Finnhub's {@code /stock/insider-transactions} response:
 * <pre>{ "data": [ ... ], "symbol": "TSLA" }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InsiderTransactionsResponse(
        List<InsiderTransaction> data,
        String symbol
) {
    public List<InsiderTransaction> data() {
        return data == null ? List.of() : data;
    }
}
