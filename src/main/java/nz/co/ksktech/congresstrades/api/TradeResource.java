package nz.co.ksktech.congresstrades.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import nz.co.ksktech.congresstrades.api.dto.TradeDto;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;
import nz.co.ksktech.congresstrades.repository.TradeRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Browse disclosed trades with optional filters. */
@Path("/api/v1/trades")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Trades", description = "Disclosed congressional trades (research data, not advice)")
public class TradeResource {

  private final TradeRepository tradeRepository;

  public TradeResource(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  @GET
  @Transactional
  @Operation(
      summary =
          "List trades filtered by member, ticker, transaction type and date range "
              + "(from/to are ISO dates, e.g. 2026-05-01)")
  public List<TradeDto> list(
      @QueryParam("member") String member,
      @QueryParam("ticker") String ticker,
      @QueryParam("type") TransactionType type,
      @QueryParam("from") String from,
      @QueryParam("to") String to) {
    return tradeRepository
        .search(member, ticker, type, parseDate(from, "from"), parseDate(to, "to"))
        .stream()
        .map(TradeDto::from)
        .toList();
  }

  private LocalDate parseDate(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Invalid '" + field + "' date '" + value + "'; expected ISO yyyy-MM-dd");
    }
  }
}
