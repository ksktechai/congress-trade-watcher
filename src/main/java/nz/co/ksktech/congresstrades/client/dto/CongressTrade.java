package nz.co.ksktech.congresstrades.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * One transaction from the Congress Trading Monitor open dataset (congress.kadoa.com), parsed from
 * the official House Clerk / Senate eFD / OGE STOCK Act filings. {@code id} is a stable unique
 * filing key used directly as our {@code sourceFilingId} for idempotent ingestion.
 *
 * @see <a
 *     href="https://github.com/kadoa-org/congress-trading-monitor">kadoa-org/congress-trading-monitor
 *     (MIT)</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CongressTrade(
    String id,
    @JsonProperty("filer_name") String filerName,
    String chamber,
    String party,
    String state,
    String ticker,
    @JsonProperty("asset_name") String assetName,
    @JsonProperty("transaction_type") String transactionType,
    @JsonProperty("amount_range_low") BigDecimal amountRangeLow,
    @JsonProperty("amount_range_high") BigDecimal amountRangeHigh,
    @JsonProperty("transaction_date") String transactionDate,
    @JsonProperty("filing_date") String filingDate,
    @JsonProperty("notification_date") String notificationDate,
    @JsonProperty("is_late") Integer isLate) {}
