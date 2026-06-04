package nz.co.ksktech.congresstrades.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.api.dto.SignalDto;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;
import nz.co.ksktech.congresstrades.service.SignalService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Detected research signals (clusters, outliers, late disclosures, concentration).
 */
@Path("/api/v1/signals")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Signals", description = "Rule-based patterns for research only — never recommendations")
public class SignalResource {

    private final SignalService signalService;

    public SignalResource(SignalService signalService) {
        this.signalService = signalService;
    }

    @GET
    @Transactional
    @Operation(summary = "List detected signals, optionally filtered by type and ticker")
    public List<SignalDto> list(@QueryParam("type") SignalType type,
                                @QueryParam("ticker") String ticker) {
        return signalService.search(type, ticker).stream().map(SignalDto::from).toList();
    }

    @POST
    @Path("/detect")
    @Operation(summary = "Re-run pure-code signal detection over the look-back window and persist results")
    public List<SignalDto> detect() {
        return signalService.detectAndPersist().stream().map(SignalDto::from).toList();
    }
}
