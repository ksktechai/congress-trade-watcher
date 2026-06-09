package nz.co.ksktech.congresstrades.api;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.api.dto.IngestResultDto;
import nz.co.ksktech.congresstrades.service.CongressDataIngestionService;
import nz.co.ksktech.congresstrades.service.IngestionResult;
import nz.co.ksktech.congresstrades.service.TradeIngestionService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Manual operational triggers. The scheduler is disabled in dev/test, so this is how you ingest on
 * demand during local development.
 */
@Path("/api/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Manual triggers for local development and operations")
public class AdminResource {

  private final CongressDataIngestionService congressIngestion;
  private final TradeIngestionService finnhubIngestion;

  public AdminResource(
      CongressDataIngestionService congressIngestion, TradeIngestionService finnhubIngestion) {
    this.congressIngestion = congressIngestion;
    this.finnhubIngestion = finnhubIngestion;
  }

  @POST
  @Path("/ingest")
  @Operation(
      summary =
          "Trigger ingestion for the whole watchlist (or a single ?ticker=SYM). "
              + "?source=congress (default, free open dataset) or ?source=finnhub (premium key).")
  public IngestResultDto ingest(
      @QueryParam("source") @DefaultValue("congress") String source,
      @QueryParam("ticker") String ticker) {
    boolean single = ticker != null && !ticker.isBlank();
    IngestionResult result;
    if ("finnhub".equalsIgnoreCase(source)) {
      result = single ? finnhubIngestion.ingestTicker(ticker) : finnhubIngestion.ingestWatchlist();
    } else {
      result =
          single ? congressIngestion.ingestTicker(ticker) : congressIngestion.ingestWatchlist();
    }
    return IngestResultDto.from(result);
  }
}
