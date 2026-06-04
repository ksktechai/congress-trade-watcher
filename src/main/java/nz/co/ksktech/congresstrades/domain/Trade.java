package nz.co.ksktech.congresstrades.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A single disclosed congressional trade.
 *
 * <p>Amounts are stored as a low/high <em>range</em> because the STOCK Act only
 * requires members to disclose a bracket (e.g. $1,001 - $15,000), never an exact
 * figure. {@code sourceFilingId} is unique so ingestion is idempotent.</p>
 */
@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trades_ticker", columnList = "ticker"),
        @Index(name = "idx_trades_member", columnList = "member_id"),
        @Index(name = "idx_trades_transaction_date", columnList = "transaction_date")
})
public class Trade extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    public Member member;

    @Column(nullable = false, length = 32)
    public String ticker;

    @Column(name = "asset_description", length = 512)
    public String assetDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    public TransactionType transactionType;

    @Column(name = "amount_range_low", precision = 19, scale = 2)
    public BigDecimal amountRangeLow;

    @Column(name = "amount_range_high", precision = 19, scale = 2)
    public BigDecimal amountRangeHigh;

    @Column(name = "transaction_date")
    public LocalDate transactionDate;

    @Column(name = "disclosure_date")
    public LocalDate disclosureDate;

    @Column(name = "days_to_disclose")
    public Integer daysToDisclose;

    @Column(name = "source_filing_id", unique = true, length = 255)
    public String sourceFilingId;

    /**
     * Midpoint of the disclosed amount range, used by signal/analysis code as a
     * single representative figure. Returns {@code null} when no range is known.
     */
    public BigDecimal estimatedAmount() {
        if (amountRangeLow == null && amountRangeHigh == null) {
            return null;
        }
        if (amountRangeLow == null) {
            return amountRangeHigh;
        }
        if (amountRangeHigh == null) {
            return amountRangeLow;
        }
        return amountRangeLow.add(amountRangeHigh)
                .divide(BigDecimal.valueOf(2));
    }

    /**
     * Recomputes {@link #daysToDisclose} from the transaction and disclosure
     * dates. Safe to call repeatedly.
     */
    public void recomputeDaysToDisclose() {
        if (transactionDate != null && disclosureDate != null) {
            this.daysToDisclose = (int) ChronoUnit.DAYS.between(transactionDate, disclosureDate);
        }
    }
}
