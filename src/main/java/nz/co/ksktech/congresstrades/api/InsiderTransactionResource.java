package nz.co.ksktech.congresstrades.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import nz.co.ksktech.congresstrades.api.dto.InsiderTransactionsResponseDto;
import nz.co.ksktech.congresstrades.client.FinnhubClient;
import nz.co.ksktech.congresstrades.client.dto.InsiderTransaction;
import nz.co.ksktech.congresstrades.client.dto.InsiderTransactionsResponse;
import nz.co.ksktech.congresstrades.config.AppConfig;
import nz.co.ksktech.congresstrades.config.Disclaimers;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Corporate-insider (SEC Form 3/4/5) transactions for a ticker, fetched live from Finnhub. This is
 * a <strong>Finnhub-only</strong> passthrough — it is not part of the congressional dataset and is
 * not persisted. Requires {@code FINNHUB_API_KEY}.
 *
 * <p>Research data only — NOT financial advice.
 */
@Path("/api/v1/insider-transactions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(
    name = "Insider transactions",
    description = "Corporate insider trades via Finnhub (live, not persisted)")
public class InsiderTransactionResource {

  private final FinnhubClient finnhubClient;
  private final AppConfig appConfig;

  public InsiderTransactionResource(@RestClient FinnhubClient finnhubClient, AppConfig appConfig) {
    this.finnhubClient = finnhubClient;
    this.appConfig = appConfig;
  }

  @GET
  @Operation(
      summary =
          "Insider transactions for a ticker (Finnhub). "
              + "Required: ?symbol=TSLA. Optional: &from=2026-01-01&to=2026-06-05 (ISO dates).")
  public InsiderTransactionsResponseDto get(
      @QueryParam("symbol") String symbol,
      @QueryParam("from") String from,
      @QueryParam("to") String to) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException(
          "Query parameter 'symbol' is required (e.g. ?symbol=TSLA).");
    }
    String token =
        appConfig
            .finnhub()
            .apiKey()
            .filter(k -> !k.isBlank())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "FINNHUB_API_KEY is not configured; the insider-transactions endpoint requires a Finnhub key."));

    String ticker = symbol.toUpperCase();
    InsiderTransactionsResponse response =
        finnhubClient.getInsiderTransactions(ticker, from, to, token);
    List<InsiderTransaction> transactions = response.data();

    return new InsiderTransactionsResponseDto(
        ticker, from, to, transactions.size(), transactions, Disclaimers.NOT_FINANCIAL_ADVICE);
  }
}
