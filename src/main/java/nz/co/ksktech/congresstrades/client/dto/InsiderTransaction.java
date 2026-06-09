package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * One corporate-insider transaction from Finnhub's {@code /stock/insider-transactions} endpoint
 * (SEC Form 3/4/5 data). This is a company insider (officer/director), NOT a member of Congress.
 *
 * <p>{@code transactionCode} follows SEC codes (e.g. {@code P}=purchase, {@code S}=sale, {@code
 * M}=option exercise, {@code A}=grant, {@code G}=gift, {@code D}=disposition).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InsiderTransaction(
    String name,
    Long share,
    Long change,
    @JsonProperty("filingDate") String filingDate,
    @JsonProperty("transactionDate") String transactionDate,
    @JsonProperty("transactionCode") String transactionCode,
    @JsonProperty("transactionPrice") BigDecimal transactionPrice,
    @JsonProperty("isDerivative") Boolean isDerivative,
    String currency,
    String source,
    String symbol,
    String id) {}
