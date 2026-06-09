package nz.co.ksktech.congresstrades.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import nz.co.ksktech.congresstrades.api.dto.MemberDto;
import nz.co.ksktech.congresstrades.api.dto.TradeDto;
import nz.co.ksktech.congresstrades.api.exception.ResourceNotFoundException;
import nz.co.ksktech.congresstrades.repository.MemberRepository;
import nz.co.ksktech.congresstrades.repository.TradeRepository;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Browse members and their disclosed trades. */
@Path("/api/v1/members")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Members", description = "Congress members and their disclosures")
public class MemberResource {

  private final MemberRepository memberRepository;
  private final TradeRepository tradeRepository;

  public MemberResource(MemberRepository memberRepository, TradeRepository tradeRepository) {
    this.memberRepository = memberRepository;
    this.tradeRepository = tradeRepository;
  }

  @GET
  @Transactional
  @Operation(summary = "List all known members")
  public List<MemberDto> list() {
    return memberRepository.listAll().stream().map(MemberDto::from).toList();
  }

  @GET
  @Path("/{name}/trades")
  @Transactional
  @Operation(summary = "List a member's disclosed trades by full name")
  public List<TradeDto> tradesForMember(@PathParam("name") String name) {
    memberRepository
        .findByFullName(name)
        .orElseThrow(() -> new ResourceNotFoundException("No member named '" + name + "'"));
    return tradeRepository.findByMemberFullName(name).stream().map(TradeDto::from).toList();
  }
}
