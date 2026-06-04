package nz.co.ksktech.congresstrades.service;

import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.domain.Member;
import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.Chamber;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for the deterministic signal rules — no Quarkus, no DB, no LLM.
 */
class SignalDetectionServiceTest {

    private final SignalDetectionService service = new SignalDetectionService(defaultConfig());

    @Test
    void cluster_detectedWhenThreeMembersBuySameTickerInWindow() {
        List<Trade> trades = List.of(
                buy("Alice", "AAPL", LocalDate.of(2026, 5, 1)),
                buy("Bob", "AAPL", LocalDate.of(2026, 5, 5)),
                buy("Carol", "AAPL", LocalDate.of(2026, 5, 10)));

        List<Signal> signals = service.detectClusters(trades);

        assertEquals(1, signals.size());
        assertEquals(SignalType.CLUSTER, signals.get(0).signalType);
        assertEquals("AAPL", signals.get(0).ticker);
        assertEquals(3.0, signals.get(0).score);
    }

    @Test
    void cluster_notDetectedWhenOutsideWindow() {
        List<Trade> trades = List.of(
                buy("Alice", "AAPL", LocalDate.of(2026, 5, 1)),
                buy("Bob", "AAPL", LocalDate.of(2026, 5, 5)),
                buy("Carol", "AAPL", LocalDate.of(2026, 6, 30)));

        assertTrue(service.detectClusters(trades).isEmpty());
    }

    @Test
    void cluster_notDetectedForSalesOnly() {
        List<Trade> trades = List.of(
                trade("Alice", "AAPL", TransactionType.SALE, 1000, 5000, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5)),
                trade("Bob", "AAPL", TransactionType.SALE, 1000, 5000, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 6)),
                trade("Carol", "AAPL", TransactionType.SALE, 1000, 5000, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 7)));

        assertTrue(service.detectClusters(trades).isEmpty());
    }

    @Test
    void outlier_detectedWhenTradeFarAboveMemberMedian() {
        List<Trade> trades = List.of(
                trade("Alice", "AAPL", TransactionType.PURCHASE, 1000, 1000, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5)),
                trade("Alice", "MSFT", TransactionType.PURCHASE, 1000, 1000, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 5)),
                trade("Alice", "TSLA", TransactionType.PURCHASE, 1000, 1000, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)),
                trade("Alice", "NVDA", TransactionType.PURCHASE, 50000, 50000, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)));

        List<Signal> signals = service.detectOutliers(trades);

        assertEquals(1, signals.size());
        assertEquals(SignalType.OUTLIER, signals.get(0).signalType);
        assertEquals("NVDA", signals.get(0).ticker);
        assertTrue(signals.get(0).score >= 3.0);
    }

    @Test
    void outlier_notDetectedWithoutEnoughHistory() {
        List<Trade> trades = List.of(
                trade("Alice", "AAPL", TransactionType.PURCHASE, 1000, 1000, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5)),
                trade("Alice", "NVDA", TransactionType.PURCHASE, 50000, 50000, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5)));

        assertTrue(service.detectOutliers(trades).isEmpty());
    }

    @Test
    void lateDisclosure_detectedWhenBeyondThreshold() {
        Trade late = trade("Alice", "AAPL", TransactionType.PURCHASE, 1000, 5000,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 15)); // 45 days
        Trade onTime = trade("Bob", "MSFT", TransactionType.PURCHASE, 1000, 5000,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)); // 9 days

        List<Signal> signals = service.detectLateDisclosures(List.of(late, onTime));

        assertEquals(1, signals.size());
        assertEquals(SignalType.LATE_DISCLOSURE, signals.get(0).signalType);
        assertEquals("AAPL", signals.get(0).ticker);
    }

    @Test
    void sectorConcentration_detectedWhenMemberRepeatsTickerInWindow() {
        List<Trade> trades = List.of(
                buy("Alice", "XOM", LocalDate.of(2026, 5, 1)),
                buy("Alice", "XOM", LocalDate.of(2026, 5, 10)),
                buy("Alice", "XOM", LocalDate.of(2026, 5, 20)));

        List<Signal> signals = service.detectSectorConcentration(trades);

        assertEquals(1, signals.size());
        assertEquals(SignalType.SECTOR_CONCENTRATION, signals.get(0).signalType);
        assertEquals("XOM", signals.get(0).ticker);
    }

    @Test
    void detectAll_combinesRules() {
        List<Trade> trades = List.of(
                buy("Alice", "AAPL", LocalDate.of(2026, 5, 1)),
                buy("Bob", "AAPL", LocalDate.of(2026, 5, 5)),
                buy("Carol", "AAPL", LocalDate.of(2026, 5, 10)));

        List<Signal> all = service.detectAll(trades);
        assertFalse(all.isEmpty());
        assertTrue(all.stream().anyMatch(s -> s.signalType == SignalType.CLUSTER));
    }

    // ----- helpers -----

    private Trade buy(String member, String ticker, LocalDate date) {
        return trade(member, ticker, TransactionType.PURCHASE, 1000, 5000, date, date.plusDays(5));
    }

    private Trade trade(String memberName, String ticker, TransactionType type,
                        long low, long high, LocalDate txDate, LocalDate disclosureDate) {
        Trade t = new Trade();
        t.member = new Member(memberName, Chamber.HOUSE, "I", "CA");
        t.ticker = ticker;
        t.transactionType = type;
        t.amountRangeLow = BigDecimal.valueOf(low);
        t.amountRangeHigh = BigDecimal.valueOf(high);
        t.transactionDate = txDate;
        t.disclosureDate = disclosureDate;
        t.recomputeDaysToDisclose();
        return t;
    }

    private static AppConfig.Signals defaultConfig() {
        return new AppConfig.Signals() {
            public int lookbackDays() { return 45; }
            public int clusterMinMembers() { return 3; }
            public int clusterWindowDays() { return 14; }
            public double outlierMultiplier() { return 3.0; }
            public int lateDisclosureThreshold() { return 40; }
            public int concentrationWindowDays() { return 30; }
            public int concentrationMinTrades() { return 3; }
        };
    }
}
