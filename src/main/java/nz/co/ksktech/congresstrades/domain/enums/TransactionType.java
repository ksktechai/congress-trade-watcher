package nz.co.ksktech.congresstrades.domain.enums;

import java.util.Locale;

/**
 * Normalised transaction type. Finnhub reports free-text values such as "Purchase", "Sale", "Sale
 * (Full)", "Sale (Partial)", "Exchange"; these are mapped onto this stable enum during ingestion.
 */
public enum TransactionType {
  PURCHASE,
  SALE,
  EXCHANGE,
  UNKNOWN;

  /** Best-effort mapping of a raw Finnhub transaction-type string. */
  public static TransactionType fromRaw(String raw) {
    if (raw == null || raw.isBlank()) {
      return UNKNOWN;
    }
    String value = raw.toLowerCase(Locale.ROOT);
    if (value.contains("purchase") || value.contains("buy")) {
      return PURCHASE;
    }
    if (value.contains("sale") || value.contains("sell") || value.contains("sold")) {
      return SALE;
    }
    if (value.contains("exchange")) {
      return EXCHANGE;
    }
    return UNKNOWN;
  }
}
