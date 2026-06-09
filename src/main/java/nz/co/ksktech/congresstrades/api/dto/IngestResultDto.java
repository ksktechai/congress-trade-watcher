package nz.co.ksktech.congresstrades.api.dto;

import java.util.List;
import nz.co.ksktech.congresstrades.service.IngestionResult;

/** API view of an ingestion run. */
public record IngestResultDto(
    int tickersProcessed,
    int tradesFetched,
    int newTrades,
    int updatedTrades,
    List<String> errors) {
  public static IngestResultDto from(IngestionResult r) {
    return new IngestResultDto(
        r.tickersProcessed, r.tradesFetched, r.newTrades, r.updatedTrades, r.errors);
  }
}
