package nz.co.ksktech.congresstrades.api;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nz.co.ksktech.congresstrades.api.dto.DigestResponse;
import nz.co.ksktech.congresstrades.service.DigestService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;

/**
 * The LLM-narrated daily digest.
 */
@Path("/api/v1/digest")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Digest", description = "Plain-English daily briefing (narration only, not financial advice)")
public class DigestResource {

    private final DigestService digestService;

    public DigestResource(DigestService digestService) {
        this.digestService = digestService;
    }

    @GET
    @Path("/daily")
    @Operation(summary = "Daily digest: LLM narrative over computed trades and signals, plus disclaimer. "
            + "Cached per day so repeated calls do not re-bill the LLM. Pass ?refresh=true to force a "
            + "fresh LLM call (and the full request/response log sequence).")
    public DigestResponse daily(@QueryParam("refresh") @DefaultValue("false") boolean refresh) {
        if (refresh) {
            digestService.clearCache();
        }
        return digestService.dailyDigest(LocalDate.now());
    }
}
