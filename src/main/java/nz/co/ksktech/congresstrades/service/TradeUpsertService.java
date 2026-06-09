package nz.co.ksktech.congresstrades.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import nz.co.ksktech.congresstrades.domain.Member;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.Chamber;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;
import nz.co.ksktech.congresstrades.repository.MemberRepository;
import nz.co.ksktech.congresstrades.repository.TradeRepository;

/**
 * Source-agnostic, idempotent persistence for trades. Every ingestion source normalises its raw
 * records into a {@link NormalizedTrade} and calls {@link #upsert}, so dedupe/upsert logic lives in
 * exactly one place.
 *
 * <p>Idempotency is keyed on {@code sourceFilingId}: re-ingesting the same disclosure updates the
 * existing row instead of duplicating it.
 */
@ApplicationScoped
public class TradeUpsertService {

  @Inject MemberRepository memberRepository;

  @Inject TradeRepository tradeRepository;

  /** A trade flattened to the fields this app stores, independent of any provider. */
  public record NormalizedTrade(
      String memberName,
      Chamber chamber,
      String party,
      String state,
      String ticker,
      String assetDescription,
      TransactionType transactionType,
      BigDecimal amountRangeLow,
      BigDecimal amountRangeHigh,
      LocalDate transactionDate,
      LocalDate disclosureDate,
      String sourceFilingId) {}

  /**
   * Insert or update a single trade. Members are upserted by full name and enriched with
   * chamber/party/state when better information arrives. Skips records that cannot be attributed
   * (no member or no stable id).
   */
  @Transactional
  public void upsert(NormalizedTrade nt, IngestionResult result) {
    if (nt.memberName() == null
        || nt.memberName().isBlank()
        || nt.sourceFilingId() == null
        || nt.sourceFilingId().isBlank()) {
      return;
    }
    // Real disclosures (parsed from PDFs) can contain very long, noisy text.
    // Truncate every string to its column width so no source data can break
    // ingestion on a length-overflow.
    final String memberName = trunc(nt.memberName().trim(), 255);
    final String party = trunc(nt.party(), 50);
    final String state = trunc(nt.state(), 50);
    final String filingId = trunc(nt.sourceFilingId(), 255);

    Member member =
        memberRepository
            .findByFullName(memberName)
            .orElseGet(
                () -> {
                  Member m = new Member(memberName, nt.chamber(), party, state);
                  memberRepository.persist(m);
                  return m;
                });
    // Enrich a sparsely-known member when a richer source supplies details.
    if ((member.chamber == null || member.chamber == Chamber.UNKNOWN) && nt.chamber() != null) {
      member.chamber = nt.chamber();
    }
    if (member.party == null && party != null) {
      member.party = party;
    }
    if (member.state == null && state != null) {
      member.state = state;
    }

    Trade trade = tradeRepository.findBySourceFilingId(filingId).orElse(null);
    boolean isNew = trade == null;
    if (isNew) {
      trade = new Trade();
      trade.sourceFilingId = filingId;
    }

    trade.member = member;
    trade.ticker = trunc(nt.ticker(), 32);
    trade.assetDescription = trunc(nt.assetDescription(), 512);
    trade.transactionType = nt.transactionType();
    trade.amountRangeLow = nt.amountRangeLow();
    trade.amountRangeHigh = nt.amountRangeHigh();
    trade.transactionDate = nt.transactionDate();
    trade.disclosureDate = nt.disclosureDate();
    trade.recomputeDaysToDisclose();

    if (isNew) {
      tradeRepository.persist(trade);
      result.newTrades++;
    } else {
      result.updatedTrades++;
    }
  }

  private static String trunc(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
