package nz.co.ksktech.congresstrades.api;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.api.dto.TradeDto;
import nz.co.ksktech.congresstrades.repository.TradeRepository;
import nz.co.ksktech.congresstrades.repository.WatchlistRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.List;

/**
 * Reactive (Mutiny) variants of read endpoints, demonstrating {@code Uni<T>} and
 * {@code Multi<T>}.
 *
 * <p>The rest of the app is intentionally blocking (Hibernate ORM Panache over
 * JDBC). To use those blocking repositories from a reactive endpoint without
 * blocking the I/O event loop, the blocking work is wrapped in a deferred supplier
 * and offloaded to the worker pool via {@code runSubscriptionOn(...)} — the
 * idiomatic Mutiny pattern for bridging blocking code.</p>
 */
@Path("/api/v1/reactive")
@Tag(name = "Reactive", description = "Mutiny (Uni/Multi) demonstration endpoints")
public class ReactiveDemoResource {

    private final TradeRepository tradeRepository;
    private final WatchlistRepository watchlistRepository;

    public ReactiveDemoResource(TradeRepository tradeRepository, WatchlistRepository watchlistRepository) {
        this.tradeRepository = tradeRepository;
        this.watchlistRepository = watchlistRepository;
    }

    /**
     * {@code Uni<T>}: trades for a ticker, computed off the event loop.
     */
    @GET
    @Path("/trades")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reactive trades by ticker — returns Uni<List<TradeDto>>; the blocking "
            + "JDBC query runs on the worker pool")
    public Uni<List<TradeDto>> trades(@QueryParam("ticker") String ticker) {
        return Uni.createFrom().item(() -> QuarkusTransaction.requiringNew().call(() ->
                        (ticker == null || ticker.isBlank()
                                ? tradeRepository.listAll()
                                : tradeRepository.findByTicker(ticker))
                                .stream().map(TradeDto::from).toList()))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /**
     * {@code Multi<T>}: stream the active watchlist tickers as Server-Sent Events.
     */
    @GET
    @Path("/tickers")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @Operation(summary = "Reactive stream of active watchlist tickers as SSE — returns Multi<String>")
    public Multi<String> streamTickers() {
        return Multi.createFrom().<String>emitter(emitter -> {
            List<String> tickers = QuarkusTransaction.requiringNew().call(watchlistRepository::activeTickers);
            tickers.forEach(emitter::emit);
            emitter.complete();
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
