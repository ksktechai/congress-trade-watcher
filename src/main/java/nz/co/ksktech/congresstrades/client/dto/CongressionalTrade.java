package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * One element of the Finnhub congressional-trading {@code data} array. Field names mirror the real
 * API exactly; dates arrive as ISO {@code yyyy-MM-dd} strings and are parsed during ingestion.
 *
 * <p>Example element:
 *
 * <pre>
 * {
 *   "name": "Jefferson Shreve",
 *   "amountFrom": 1001, "amountTo": 15000,
 *   "assetName": "Apple Inc",
 *   "transactionType": "Purchase",
 *   "transactionDate": "2024-08-13",
 *   "filingDate": "2024-09-17",
 *   "ownerType": "", "position": ""
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CongressionalTrade(
    String name,
    @JsonProperty("amountFrom") BigDecimal amountFrom,
    @JsonProperty("amountTo") BigDecimal amountTo,
    @JsonProperty("assetName") String assetName,
    @JsonProperty("transactionType") String transactionType,
    @JsonProperty("transactionDate") String transactionDate,
    @JsonProperty("filingDate") String filingDate,
    @JsonProperty("ownerType") String ownerType,
    String position) {}
