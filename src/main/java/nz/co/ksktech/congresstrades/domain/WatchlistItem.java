package nz.co.ksktech.congresstrades.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A ticker the ingestion job tracks. Seeded by Flyway ({@code V4__seed_watchlist}) and queried by
 * {@code TradeIngestionService} to decide which symbols to poll.
 */
@Entity
@Table(name = "watchlist")
public class WatchlistItem extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 32)
  public String ticker;

  @Column(length = 255)
  public String label;

  @Column(nullable = false)
  public boolean active = true;
}
