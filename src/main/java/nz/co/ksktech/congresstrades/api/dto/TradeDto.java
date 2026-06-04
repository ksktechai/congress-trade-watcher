package nz.co.ksktech.congresstrades.api.dto;

import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read model for a {@link Trade}. Flattens the member relationship so the API
 * never leaks lazy JPA proxies.
 */
public record TradeDto(
        Long id,
        String memberName,
        String chamber,
        String ticker,
        String assetDescription,
        TransactionType transactionType,
        BigDecimal amountRangeLow,
        BigDecimal amountRangeHigh,
        LocalDate transactionDate,
        LocalDate disclosureDate,
        Integer daysToDisclose
) {
    public static TradeDto from(Trade t) {
        return new TradeDto(
                t.id,
                t.member != null ? t.member.fullName : null,
                t.member != null && t.member.chamber != null ? t.member.chamber.name() : null,
                t.ticker,
                t.assetDescription,
                t.transactionType,
                t.amountRangeLow,
                t.amountRangeHigh,
                t.transactionDate,
                t.disclosureDate,
                t.daysToDisclose);
    }
}
