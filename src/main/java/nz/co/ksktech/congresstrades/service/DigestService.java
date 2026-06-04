package nz.co.ksktech.congresstrades.service;

import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nz.co.ksktech.congresstrades.api.dto.DigestResponse;
import nz.co.ksktech.congresstrades.api.dto.SignalDto;
import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.config.Disclaimers;
import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.repository.TradeRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Builds the daily digest: gathers the look-back trades, ensures signals are
 * fresh, asks {@link LlmInsightService} to narrate them, and assembles the
 * response with the mandatory disclaimer.
 *
 * <p>The result is cached per date ({@code @CacheResult}) so repeated calls in a
 * day do not re-bill the Anthropic API.</p>
 */
@ApplicationScoped
public class DigestService {

    @Inject
    TradeRepository tradeRepository;

    @Inject
    SignalService signalService;

    @Inject
    LlmInsightService llmInsightService;

    @Inject
    AppConfig appConfig;

    /**
     * Generate (or return cached) digest for the given date.
     */
    @CacheResult(cacheName = "daily-digest")
    public DigestResponse dailyDigest(LocalDate date) {
        LocalDate since = date.minusDays(appConfig.signals().lookbackDays());
        List<Trade> trades = loadTrades(since);

        // Refresh signals from current data, then read them back.
        List<Signal> signals = signalService.detectAndPersist();

        String narrative = llmInsightService.generateNarrative(date, trades, signals);

        List<SignalDto> signalDtos = signals.stream().map(SignalDto::from).toList();
        return new DigestResponse(date, narrative, trades.size(), signalDtos, Disclaimers.NOT_FINANCIAL_ADVICE);
    }

    @Transactional
    List<Trade> loadTrades(LocalDate since) {
        return tradeRepository.findDisclosedOnOrAfter(since);
    }
}
