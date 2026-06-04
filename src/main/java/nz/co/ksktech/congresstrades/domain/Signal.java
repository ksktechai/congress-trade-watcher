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
import jakarta.persistence.Table;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A detected, research-only pattern across one or more {@link Trade}s.
 *
 * <p>{@code relatedTradeIds} is stored as a comma-separated list to keep the
 * schema simple; {@link #relatedTradeIdList()} parses it back.</p>
 */
@Entity
@Table(name = "signals", indexes = {
        @Index(name = "idx_signals_ticker", columnList = "ticker"),
        @Index(name = "idx_signals_type", columnList = "signal_type")
})
public class Signal extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 30)
    public SignalType signalType;

    @Column(length = 32)
    public String ticker;

    @Column(length = 1024)
    public String description;

    @Column(name = "detected_at")
    public LocalDateTime detectedAt;

    @Column(name = "related_trade_ids", length = 512)
    public String relatedTradeIds;

    public Double score;

    public Signal() {
    }

    public Signal(SignalType signalType, String ticker, String description, Double score, List<Long> relatedTradeIds) {
        this.signalType = signalType;
        this.ticker = ticker;
        this.description = description;
        this.score = score;
        this.detectedAt = LocalDateTime.now();
        setRelatedTradeIdList(relatedTradeIds);
    }

    public void setRelatedTradeIdList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            this.relatedTradeIds = null;
            return;
        }
        this.relatedTradeIds = ids.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public List<Long> relatedTradeIdList() {
        if (relatedTradeIds == null || relatedTradeIds.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(relatedTradeIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
