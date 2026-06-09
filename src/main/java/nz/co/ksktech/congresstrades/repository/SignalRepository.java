package nz.co.ksktech.congresstrades.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import nz.co.ksktech.congresstrades.domain.Signal;
import nz.co.ksktech.congresstrades.domain.enums.SignalType;

@ApplicationScoped
public class SignalRepository implements PanacheRepository<Signal> {

  public List<Signal> search(SignalType type, String ticker) {
    StringBuilder query = new StringBuilder("1=1");
    Parameters params = new Parameters();
    if (type != null) {
      query.append(" and signalType = :type");
      params.and("type", type);
    }
    if (ticker != null && !ticker.isBlank()) {
      query.append(" and upper(ticker) = :ticker");
      params.and("ticker", ticker.toUpperCase());
    }
    return find(query.toString(), Sort.by("detectedAt").descending(), params).list();
  }

  public List<Signal> detectedOnOrAfter(LocalDateTime since) {
    return find("detectedAt >= ?1", Sort.by("detectedAt").descending(), since).list();
  }
}
