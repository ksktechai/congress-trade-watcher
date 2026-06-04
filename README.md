# Congress Trade Watcher

A Quarkus 3 application that ingests US Congressional stock-trade disclosures
(STOCK Act filings via the **Finnhub** API), persists and analyses them in
**PostgreSQL**, detects rule-based research patterns in pure code, and generates
a plain-English daily digest using the **Anthropic Claude** API.

> ## ⚠️ This is a research and learning tool — NOT financial advice
>
> Congressional trades are disclosed **up to 45 days late**, amounts are reported
> only as **broad ranges** (e.g. $1,001–$15,000), not exact figures, and following
> these trades **does not reliably beat the market**. The signals here surface
> patterns for human research only. The LLM is used **solely to summarise and
> contextualise** data the app has already computed — never to pick stocks or
> predict prices. Do your own due diligence and consult a licensed professional
> before making any investment decision.

---

## Requirements

- **Java 17** — the build compiles with `release 17` (JDK 21 also works fine).
  On JDKs newer than the extensions officially support (e.g. 25), Hibernate's
  ByteBuddy needs its *experimental* flag; that is pre-wired in `.mvn/jvm.config`
  and the Surefire/Failsafe config, so `./mvnw verify` still runs there too.
- **Docker** + **Docker Compose** (PostgreSQL, and Testcontainers for the test suite).
- Maven is **not** required — use the bundled `./mvnw` wrapper.

## Quick start

```bash
# 1. Configure secrets (never committed)
cp .env.example .env          # then edit .env with your real keys
export FINNHUB_API_KEY=...     # https://finnhub.io  (free signup)
export ANTHROPIC_API_KEY=...   # https://console.anthropic.com

# 2. Start PostgreSQL
docker compose up -d

# 3. Build, run tests (Testcontainers + WireMock — no real API calls), and package
./mvnw verify

# 4. Run the app in dev mode (live reload; scheduler disabled in dev)
./mvnw quarkus:dev
```

The API is then at <http://localhost:8080>, with:

- **Swagger UI**: <http://localhost:8080/q/swagger-ui>
- **OpenAPI**: <http://localhost:8080/q/openapi>
- **Health**: <http://localhost:8080/q/health> (readiness includes a DB check)

### Setting the two required environment variables

| Variable            | Where to get it                               | Used for                          |
|---------------------|-----------------------------------------------|-----------------------------------|
| `FINNHUB_API_KEY`   | https://finnhub.io → Dashboard → API Keys     | Congressional trades + quotes     |
| `ANTHROPIC_API_KEY` | https://console.anthropic.com → API Keys      | Digest narration (Claude)         |

They are referenced in `application.properties` as `${FINNHUB_API_KEY}` /
`${ANTHROPIC_API_KEY}` and read from the environment. **No secret is ever stored
in a properties file.**

## Trigger ingestion manually (dev)

The scheduler is **disabled in dev** so local runs never auto-hit the external
APIs. Trigger ingestion yourself:

```bash
# Ingest the whole seeded watchlist (AAPL, MSFT, NVDA, ... LMT)
curl -X POST "http://localhost:8080/api/v1/admin/ingest"

# Ingest a single symbol
curl -X POST "http://localhost:8080/api/v1/admin/ingest?ticker=AAPL"
```

In `%prod` the scheduler runs on `ingestion.cron` (default every 30 minutes).

## API endpoints & sample requests

```bash
# Trades — filter by member, ticker, type (PURCHASE|SALE|EXCHANGE), date range
curl "http://localhost:8080/api/v1/trades"
curl "http://localhost:8080/api/v1/trades?ticker=AAPL&type=PURCHASE"
curl "http://localhost:8080/api/v1/trades?member=Pelosi&from=2026-01-01&to=2026-06-01"

# Members
curl "http://localhost:8080/api/v1/members"
curl "http://localhost:8080/api/v1/members/Jane%20Representative/trades"

# Signals (clusters / outliers / late disclosures / concentration)
curl "http://localhost:8080/api/v1/signals"
curl "http://localhost:8080/api/v1/signals?type=CLUSTER&ticker=AAPL"
curl -X POST "http://localhost:8080/api/v1/signals/detect"   # re-run detection

# Daily digest — LLM narrative + structured signals + disclaimer (cached per day)
curl "http://localhost:8080/api/v1/digest/daily"
```

## Architecture

```
            ┌──────────────┐         ┌──────────────────────────────┐
  cron ───► │  Scheduler   │         │           REST API           │
            └──────┬───────┘         │ Trade / Member / Signal /    │
                   │                 │ Digest / Admin resources     │
                   ▼                 └───────────────┬──────────────┘
        ┌───────────────────────────────────────────▼──────────────┐
        │                       Service layer                       │
        │  Ingestion · SignalDetection (pure) · Performance · LLM   │
        └───┬───────────────┬──────────────────┬──────────────┬─────┘
            │               │                  │              │ (cache)
            ▼               ▼                  ▼              ▼
   ┌────────────────┐  ┌──────────┐    ┌──────────────┐  ┌──────────┐
   │ Finnhub  (FT)  │  │ Postgres │    │ Anthropic FT │  │  digest  │
   │ trades+quotes  │  │ (Flyway) │    │   Claude     │  │  cache   │
   └────────────────┘  └──────────┘    └──────────────┘  └──────────┘
   FT = fault-tolerance ring (@Retry + @Timeout + @CircuitBreaker)
```

Diagrams: [`docs/architecture.puml`](docs/architecture.puml) (PlantUML) and
[`docs/architecture.drawio`](docs/architecture.drawio) (draw.io).

### Layering

- **api** — JAX-RS resources + DTOs + exception mapping. No business logic.
- **service** — all logic. `SignalDetectionService` is **pure code** (no DB/LLM)
  and fully unit-tested; the LLM only narrates its output.
- **client** — `@RegisterRestClient` typed clients with fault tolerance.
- **repository** — Panache repositories.
- **domain** — Panache entities (public fields) + enums.

## How it works

1. **Ingestion** (`TradeIngestionService`) polls Finnhub per watchlist ticker,
   upserts members, and persists trades idempotently using a deterministic
   `sourceFilingId` (Finnhub exposes no stable id).
2. **Signal detection** (`SignalDetectionService`, pure code) finds:
   `CLUSTER`, `OUTLIER`, `LATE_DISCLOSURE`, `SECTOR_CONCENTRATION`.
3. **Digest** (`DigestService` + `LlmInsightService`) builds a structured prompt
   from the computed trades/signals and asks Claude to write a neutral briefing.
   The result is cached per day so repeated calls don't re-bill the API.

## Testing

```bash
./mvnw verify
```

- `SignalDetectionServiceTest` — pure unit tests, one per rule.
- `TradeResourceTest` — `@QuarkusTest` + REST Assured over Testcontainers Postgres.
- `LlmInsightServiceTest` — WireMock stands in for Anthropic; the real API is
  **never** called.

All external HTTP is mocked via WireMock; Postgres is provided by Quarkus Dev
Services (Testcontainers). **No real Finnhub or Anthropic calls happen in tests.**

## Database UI (optional)

```bash
docker compose --profile tools up -d   # pgAdmin at http://localhost:5050
```

## Data sources & honesty

- Finnhub Congressional Trading: <https://finnhub.io/docs/api/congressional-trading>
- Anthropic Messages API: <https://docs.anthropic.com/en/api/messages>

This project exists to **learn** and to **research disclosure patterns**, not to
generate trading signals. Treat every output accordingly.
