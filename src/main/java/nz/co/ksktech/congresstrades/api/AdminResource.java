package nz.co.ksktech.congresstrades.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.api.dto.IngestResultDto;
import nz.co.ksktech.congresstrades.service.TradeIngestionService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Manual operational triggers. The scheduler is disabled in dev/test, so this is
 * how you ingest on demand during local development.
 */
@Path("/api/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Manual triggers for local development and operations")
public class AdminResource {

    private final TradeIngestionService ingestionService;

    public AdminResource(TradeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @POST
    @Path("/ingest")
    @Operation(summary = "Trigger ingestion for the whole watchlist (or a single ?ticker=SYM)")
    public IngestResultDto ingest(@QueryParam("ticker") String ticker) {
        TradeIngestionService.IngestionResult result =
                (ticker == null || ticker.isBlank())
                        ? ingestionService.ingestWatchlist()
                        : ingestionService.ingestTicker(ticker);
        return IngestResultDto.from(result);
    }
}
