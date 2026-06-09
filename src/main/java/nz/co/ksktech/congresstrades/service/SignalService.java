package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;
import nz.co.ksktech.congresstrades.repository.SignalRepository;
import nz.co.ksktech.congresstrades.repository.TradeRepository;
import org.jboss.logging.Logger;

/**
 * Orchestrates the pure-code {@link SignalDetectionService} against persisted trades: loads the
 * look-back window, runs detection, and replaces the stored signal set so repeated runs stay
 * idempotent.
 */
@ApplicationScoped
public class SignalService {

  private static final Logger LOG = Logger.getLogger(SignalService.class);

  @Inject TradeRepository tradeRepository;

  @Inject SignalRepository signalRepository;

  @Inject SignalDetectionService detectionService;

  @Inject AppConfig appConfig;

  /**
   * Detect over the configured look-back window and persist the results, replacing any previously
   * stored signals.
   */
  @Transactional
  public List<Signal> detectAndPersist() {
    LocalDate since = LocalDate.now().minusDays(appConfig.signals().lookbackDays());
    List<Trade> trades = tradeRepository.findDisclosedOnOrAfter(since);
    List<Signal> detected = detectionService.detectAll(trades);

    signalRepository.deleteAll();
    for (Signal s : detected) {
      signalRepository.persist(s);
    }
    LOG.infof("Detected and persisted %d signals from %d trades", detected.size(), trades.size());
    return detected;
  }

  public List<Signal> search(SignalType type, String ticker) {
    return signalRepository.search(type, ticker);
  }
}
