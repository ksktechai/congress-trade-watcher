package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Detects research-only patterns over a set of trades.
 *
 * <p><strong>This is pure, deterministic code — no LLM, no external calls.</strong>
 * Every rule here is fully unit-testable with crafted in-memory trades and is the
 * canonical pattern for adding new analytics. The LLM only ever narrates the
 * output of this class; it never produces a signal.</p>
 *
 * <p>Rules:</p>
 * <ul>
 *   <li><b>CLUSTER</b> — N+ distinct members buying the same ticker inside a
 *       rolling window.</li>
 *   <li><b>OUTLIER</b> — a trade whose amount exceeds a multiple of the member's
 *       historical median trade size.</li>
 *   <li><b>LATE_DISCLOSURE</b> — disclosure filed beyond a day threshold (near the
 *       45-day legal limit).</li>
 *   <li><b>SECTOR_CONCENTRATION</b> — one member trading the same ticker N+ times
 *       within a window (a simplified concentration proxy).</li>
 * </ul>
 */
@ApplicationScoped
public class SignalDetectionService {

    private final AppConfig.Signals cfg;

    @Inject
    public SignalDetectionService(AppConfig appConfig) {
        this.cfg = appConfig.signals();
    }

    /** Test-friendly constructor that takes the signal config directly. */
    public SignalDetectionService(AppConfig.Signals signalsConfig) {
        this.cfg = signalsConfig;
    }

    /** Runs every rule and returns the combined, freshly-built signals. */
    public List<Signal> detectAll(List<Trade> trades) {
        List<Signal> signals = new ArrayList<>();
        signals.addAll(detectClusters(trades));
        signals.addAll(detectOutliers(trades));
        signals.addAll(detectLateDisclosures(trades));
        signals.addAll(detectSectorConcentration(trades));
        return signals;
    }

    /**
     * CLUSTER: {@code clusterMinMembers}+ distinct members buying the same ticker
     * within any {@code clusterWindowDays}-day rolling window. One signal per
     * qualifying ticker; score = peak distinct-member count.
     */
    public List<Signal> detectClusters(List<Trade> trades) {
        List<Signal> result = new ArrayList<>();
        Map<String, List<Trade>> byTicker = groupByTicker(trades.stream()
                .filter(t -> t.transactionType == TransactionType.PURCHASE)
                .filter(t -> t.transactionDate != null && t.member != null && t.member.fullName != null)
                .collect(Collectors.toList()));

        for (Map.Entry<String, List<Trade>> entry : byTicker.entrySet()) {
            List<Trade> ordered = entry.getValue().stream()
                    .sorted(Comparator.comparing(t -> t.transactionDate))
                    .collect(Collectors.toList());

            int peak = 0;
            List<Long> bestWindowTradeIds = List.of();
            for (Trade anchor : ordered) {
                Set<String> members = new TreeSet<>();
                List<Long> windowTradeIds = new ArrayList<>();
                for (Trade other : ordered) {
                    long gap = ChronoUnit.DAYS.between(anchor.transactionDate, other.transactionDate);
                    if (gap >= 0 && gap <= cfg.clusterWindowDays()) {
                        members.add(other.member.fullName);
                        if (other.id != null) {
                            windowTradeIds.add(other.id);
                        }
                    }
                }
                if (members.size() > peak) {
                    peak = members.size();
                    bestWindowTradeIds = windowTradeIds;
                }
            }

            if (peak >= cfg.clusterMinMembers()) {
                String desc = String.format(
                        "%d distinct members purchased %s within a %d-day window.",
                        peak, entry.getKey(), cfg.clusterWindowDays());
                result.add(new Signal(SignalType.CLUSTER, entry.getKey(), desc, (double) peak, bestWindowTradeIds));
            }
        }
        return result;
    }

    /**
     * OUTLIER: a trade whose estimated amount is more than
     * {@code outlierMultiplier} x the member's historical median amount.
     * Requires at least 3 priced trades for a stable median.
     */
    public List<Signal> detectOutliers(List<Trade> trades) {
        List<Signal> result = new ArrayList<>();
        Map<String, List<Trade>> byMember = trades.stream()
                .filter(t -> t.member != null && t.member.fullName != null)
                .filter(t -> t.estimatedAmount() != null)
                .collect(Collectors.groupingBy(t -> t.member.fullName, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Trade>> entry : byMember.entrySet()) {
            List<Trade> memberTrades = entry.getValue();
            if (memberTrades.size() < 3) {
                continue;
            }
            BigDecimal median = median(memberTrades.stream()
                    .map(Trade::estimatedAmount)
                    .sorted()
                    .collect(Collectors.toList()));
            if (median.signum() <= 0) {
                continue;
            }
            BigDecimal threshold = median.multiply(BigDecimal.valueOf(cfg.outlierMultiplier()));
            for (Trade t : memberTrades) {
                if (t.estimatedAmount().compareTo(threshold) > 0) {
                    double ratio = t.estimatedAmount().divide(median, 2, java.math.RoundingMode.HALF_UP).doubleValue();
                    String desc = String.format(
                            "%s's %s trade (~$%s) is %.1fx their median trade size (~$%s).",
                            entry.getKey(), t.ticker, t.estimatedAmount().toPlainString(), ratio, median.toPlainString());
                    result.add(new Signal(SignalType.OUTLIER, t.ticker, desc, ratio,
                            t.id != null ? List.of(t.id) : List.of()));
                }
            }
        }
        return result;
    }

    /**
     * LATE_DISCLOSURE: any trade whose daysToDisclose strictly exceeds the
     * configured threshold (approaching the 45-day STOCK Act limit).
     */
    public List<Signal> detectLateDisclosures(List<Trade> trades) {
        List<Signal> result = new ArrayList<>();
        for (Trade t : trades) {
            if (t.daysToDisclose != null && t.daysToDisclose > cfg.lateDisclosureThreshold()) {
                String member = t.member != null ? t.member.fullName : "Unknown member";
                String desc = String.format(
                        "%s disclosed a %s trade %d days after it occurred (limit is 45 days).",
                        member, t.ticker, t.daysToDisclose);
                result.add(new Signal(SignalType.LATE_DISCLOSURE, t.ticker, desc,
                        (double) t.daysToDisclose, t.id != null ? List.of(t.id) : List.of()));
            }
        }
        return result;
    }

    /**
     * SECTOR_CONCENTRATION (simplified): a member with {@code concentrationMinTrades}+
     * trades in the same ticker inside a {@code concentrationWindowDays}-day window.
     */
    public List<Signal> detectSectorConcentration(List<Trade> trades) {
        List<Signal> result = new ArrayList<>();
        Map<String, List<Trade>> byMemberTicker = trades.stream()
                .filter(t -> t.member != null && t.member.fullName != null && t.transactionDate != null)
                .collect(Collectors.groupingBy(t -> t.member.fullName + " " + t.ticker,
                        LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Trade>> entry : byMemberTicker.entrySet()) {
            List<Trade> ordered = entry.getValue().stream()
                    .sorted(Comparator.comparing(t -> t.transactionDate))
                    .collect(Collectors.toList());

            for (int i = 0; i < ordered.size(); i++) {
                Trade anchor = ordered.get(i);
                List<Long> windowIds = new ArrayList<>();
                int count = 0;
                for (Trade other : ordered) {
                    long gap = ChronoUnit.DAYS.between(anchor.transactionDate, other.transactionDate);
                    if (gap >= 0 && gap <= cfg.concentrationWindowDays()) {
                        count++;
                        if (other.id != null) {
                            windowIds.add(other.id);
                        }
                    }
                }
                if (count >= cfg.concentrationMinTrades()) {
                    String[] parts = entry.getKey().split(" ", 2);
                    String desc = String.format(
                            "%s made %d trades in %s within %d days.",
                            parts[0], count, parts[1], cfg.concentrationWindowDays());
                    result.add(new Signal(SignalType.SECTOR_CONCENTRATION, parts[1], desc, (double) count, windowIds));
                    break; // one signal per member+ticker is enough
                }
            }
        }
        return result;
    }

    private Map<String, List<Trade>> groupByTicker(List<Trade> trades) {
        return trades.stream()
                .filter(t -> t.ticker != null)
                .collect(Collectors.groupingBy(t -> t.ticker, LinkedHashMap::new, Collectors.toList()));
    }

    /** Median of a list assumed already sorted ascending. */
    private BigDecimal median(List<BigDecimal> sorted) {
        int n = sorted.size();
        if (n == 0) {
            return BigDecimal.ZERO;
        }
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2))
                .divide(BigDecimal.valueOf(2));
    }
}
