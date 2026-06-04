package nz.co.ksktech.congresstrades.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.co.ksktech.congresstrades.service.CongressDataIngestionService;
import nz.co.ksktech.congresstrades.service.IngestionResult;
import nz.co.ksktech.congresstrades.service.SignalService;
import nz.co.ksktech.congresstrades.service.TradeIngestionService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Periodically ingests watchlist disclosures and refreshes signals.
 *
 * <p>The cron expression comes from {@code ingestion.cron}; the source from
 * {@code ingestion.source} ({@code congress}, the free default, or
 * {@code finnhub}). In the {@code dev} and {@code test} profiles the cron is
 * {@code "off"} so local runs never auto-hit the external APIs — use
 * {@code POST /api/v1/admin/ingest} to trigger ingestion manually instead.</p>
 */
@ApplicationScoped
public class IngestionScheduler {

    private static final Logger LOG = Logger.getLogger(IngestionScheduler.class);

    @Inject
    CongressDataIngestionService congressIngestion;

    @Inject
    TradeIngestionService finnhubIngestion;

    @Inject
    SignalService signalService;

    @ConfigProperty(name = "ingestion.source", defaultValue = "congress")
    String source;

    @Scheduled(cron = "{ingestion.cron}", identity = "watchlist-ingestion")
    void scheduledIngestion() {
        LOG.infof("Scheduled ingestion starting (source=%s)", source);
        IngestionResult result = "finnhub".equalsIgnoreCase(source)
                ? finnhubIngestion.ingestWatchlist()
                : congressIngestion.ingestWatchlist();
        signalService.detectAndPersist();
        LOG.infof("Scheduled ingestion finished: %s", result);
    }
}
