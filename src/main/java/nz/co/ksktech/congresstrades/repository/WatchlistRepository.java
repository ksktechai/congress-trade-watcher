package nz.co.ksktech.congresstrades.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import nz.co.ksktech.congresstrades.domain.WatchlistItem;

import java.util.List;

@ApplicationScoped
public class WatchlistRepository implements PanacheRepository<WatchlistItem> {

    public List<WatchlistItem> activeItems() {
        return list("active", true);
    }

    public List<String> activeTickers() {
        return activeItems().stream().map(item -> item.ticker).toList();
    }
}
