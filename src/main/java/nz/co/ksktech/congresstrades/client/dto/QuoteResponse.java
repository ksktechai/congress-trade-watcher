package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Finnhub {@code /quote} response. Finnhub uses terse single-letter keys: c=current, d=change,
 * dp=percent change, h=high, l=low, o=open, pc=previous close, t=epoch seconds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuoteResponse(
    @JsonProperty("c") BigDecimal current,
    @JsonProperty("d") BigDecimal change,
    @JsonProperty("dp") BigDecimal percentChange,
    @JsonProperty("h") BigDecimal high,
    @JsonProperty("l") BigDecimal low,
    @JsonProperty("o") BigDecimal open,
    @JsonProperty("pc") BigDecimal previousClose,
    @JsonProperty("t") Long timestamp) {}
