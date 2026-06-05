package nz.co.ksktech.congresstrades.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.client.dto.CongressionalTradingResponse;
import nz.co.ksktech.congresstrades.client.dto.InsiderTransactionsResponse;
import nz.co.ksktech.congresstrades.client.dto.QuoteResponse;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

/**
 * Typed client for the Finnhub REST API.
 *
 * <p>This is the canonical example of an external client in this project:
 * a {@code @RegisterRestClient} interface, the API token passed as a
 * {@code @QueryParam} (Finnhub's real auth model), and a fault-tolerance ring
 * ({@code @Retry} + {@code @Timeout} + {@code @CircuitBreaker}) on every method.</p>
 *
 * <p>Base URL is configured via {@code quarkus.rest-client.finnhub-api.url}.</p>
 */
@RegisterRestClient(configKey = "finnhub-api")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public interface FinnhubClient {

    /**
     * Disclosed congressional trades for a single ticker.
     */
    @GET
    @Path("/stock/congressional-trading")
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 10000)
    CongressionalTradingResponse getCongressionalTrades(@QueryParam("symbol") String symbol,
                                                        @QueryParam("token") String token);

    /**
     * Real-time-ish quote for a ticker (used for price enrichment only).
     */
    @GET
    @Path("/quote")
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 10000)
    QuoteResponse getQuote(@QueryParam("symbol") String symbol,
                           @QueryParam("token") String token);

    /**
     * Corporate-insider (SEC Form 3/4/5) transactions for a ticker, optionally
     * bounded by from/to dates (ISO {@code yyyy-MM-dd}). Null date params are
     * omitted from the request.
     */
    @GET
    @Path("/stock/insider-transactions")
    @Timeout(value = 8, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 10000)
    InsiderTransactionsResponse getInsiderTransactions(@QueryParam("symbol") String symbol,
                                                       @QueryParam("from") String from,
                                                       @QueryParam("to") String to,
                                                       @QueryParam("token") String token);
}
