package nz.co.ksktech.congresstrades.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import nz.co.ksktech.congresstrades.domain.WatchlistItem;

@ApplicationScoped
public class WatchlistRepository implements PanacheRepository<WatchlistItem> {

  public List<WatchlistItem> activeItems() {
    return list("active", true);
  }

  public List<String> activeTickers() {
    return activeItems().stream().map(item -> item.ticker).toList();
  }
}
