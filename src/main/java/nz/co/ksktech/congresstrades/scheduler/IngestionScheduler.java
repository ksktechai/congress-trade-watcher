package nz.co.ksktech.congresstrades.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.service.SignalService;
import nz.co.ksktech.congresstrades.service.TradeIngestionService;
import org.jboss.logging.Logger;

/**
 * Periodically ingests watchlist disclosures and refreshes signals.
 *
 * <p>The cron expression comes from {@code ingestion.cron}. In the {@code dev}
 * and {@code test} profiles it is set to {@code "off"} so local runs never
 * auto-hit the external APIs — use {@code POST /api/v1/admin/ingest} to trigger
 * ingestion manually instead.</p>
 */
@ApplicationScoped
public class IngestionScheduler {

    private static final Logger LOG = Logger.getLogger(IngestionScheduler.class);

    @Inject
    TradeIngestionService ingestionService;

    @Inject
    SignalService signalService;

    @Scheduled(cron = "{ingestion.cron}", identity = "watchlist-ingestion")
    void scheduledIngestion() {
        LOG.info("Scheduled ingestion starting");
        TradeIngestionService.IngestionResult result = ingestionService.ingestWatchlist();
        signalService.detectAndPersist();
        LOG.infof("Scheduled ingestion finished: %s", result);
    }
}
