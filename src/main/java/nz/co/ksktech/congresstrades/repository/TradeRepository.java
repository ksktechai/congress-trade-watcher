package nz.co.ksktech.congresstrades.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import nz.co.ksktech.congresstrades.domain.Trade;
import nz.co.ksktech.congresstrades.domain.enums.TransactionType;

@ApplicationScoped
public class TradeRepository implements PanacheRepository<Trade> {

  public Optional<Trade> findBySourceFilingId(String sourceFilingId) {
    return find("sourceFilingId", sourceFilingId).firstResultOptional();
  }

  /** Dynamic, null-tolerant filter used by {@code TradeResource}. */
  public List<Trade> search(
      String memberName, String ticker, TransactionType type, LocalDate from, LocalDate to) {
    StringBuilder query = new StringBuilder("1=1");
    Parameters params = new Parameters();

    if (memberName != null && !memberName.isBlank()) {
      query.append(" and lower(member.fullName) like :member");
      params.and("member", "%" + memberName.toLowerCase() + "%");
    }
    if (ticker != null && !ticker.isBlank()) {
      query.append(" and upper(ticker) = :ticker");
      params.and("ticker", ticker.toUpperCase());
    }
    if (type != null) {
      query.append(" and transactionType = :type");
      params.and("type", type);
    }
    if (from != null) {
      query.append(" and transactionDate >= :from");
      params.and("from", from);
    }
    if (to != null) {
      query.append(" and transactionDate <= :to");
      params.and("to", to);
    }

    return find(query.toString(), Sort.by("transactionDate").descending(), params).list();
  }

  public List<Trade> findByTicker(String ticker) {
    return find("upper(ticker) = ?1", ticker.toUpperCase()).list();
  }

  public List<Trade> findByMemberFullName(String fullName) {
    return find("member.fullName", Sort.by("transactionDate").descending(), fullName).list();
  }

  /**
   * Trades disclosed on or after the given date, newest first. Used as the input set for the daily
   * digest and on-demand signal detection.
   */
  public List<Trade> findDisclosedOnOrAfter(LocalDate since) {
    return find("disclosureDate >= ?1", Sort.by("disclosureDate").descending(), since).list();
  }
}
