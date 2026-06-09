package nz.co.ksktech.congresstrades.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.temporal.ChronoUnit;
import nz.co.ksktech.congresstrades.client.dto.CongressTickerTrades;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Client for the free, no-key Congress Trading Monitor open dataset (congress.kadoa.com), which
 * serves real STOCK Act disclosures as static per-ticker JSON. Same fault-tolerance ring as the
 * other external clients.
 *
 * <p>Base URL is configured via {@code quarkus.rest-client.congress-data.url}.
 */
@RegisterRestClient(configKey = "congress-data")
@RegisterProvider(ExternalClientLoggingFilter.class)
@Path("/data")
@Produces(MediaType.APPLICATION_JSON)
public interface CongressDataClient {

  @GET
  @Path("/ticker/{ticker}.json")
  @Timeout(value = 15, unit = ChronoUnit.SECONDS)
  @Retry(maxRetries = 2, delay = 750)
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 10000)
  CongressTickerTrades getTickerTrades(@PathParam("ticker") String ticker);
}
