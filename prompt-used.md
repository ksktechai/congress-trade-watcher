# Task: Build a runnable Quarkus app — "congress-trade-watcher"

Build a complete, runnable Quarkus 3.x application (Java 17, Maven) that ingests
US Congressional stock-trade disclosures, persists and analyses them in PostgreSQL,
and generates plain-English daily digests via the Anthropic Claude API.

I must be able to run it locally end-to-end with Docker + Maven.

## Purpose & honest framing
This is a research/learning tool, NOT a trading-signal generator. Congressional
trades are disclosed up to 45 days late, amounts are ranges not exact figures,
and following them does not reliably beat the market. The app surfaces patterns
for research; it must NOT present output as financial advice. Include this
disclaimer in the README and in the digest API output.

## Domain
- Members of Congress must disclose trades under the STOCK Act.
- Data source: **Finnhub congressional trading API** (free tier, API key required).
  Docs: https://finnhub.io/docs/api/congressional-trading
  Endpoint pattern: GET https://finnhub.io/api/v1/stock/congressional-trading?symbol={ticker}&token={apiKey}
  (also supports date filtering; consult the real response shape and model DTOs to match it).
- Stock price enrichment: Finnhub quote endpoint
  GET https://finnhub.io/api/v1/quote?symbol={ticker}&token={apiKey}
- LLM narrative: **Anthropic Claude API** (https://api.anthropic.com/v1/messages),
  model claude-sonnet-4-5 (or current Sonnet), used ONLY to summarise/contextualise
  data the app has already computed — never to pick stocks or predict prices.
- Package root: `nz.co.ksktech.congresstrades`

## API keys (env vars only — NO secrets in properties files)
- FINNHUB_API_KEY
- ANTHROPIC_API_KEY
  Reference them in application.properties via ${FINNHUB_API_KEY} etc.

## Project structure
```
congress-trade-watcher/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/
├── docker-compose.yml                  # postgres:16 + pgadmin (optional)
├── .env.example                        # documents required env vars (no real values)
├── README.md
├── CLAUDE.md
├── .github/copilot-instructions.md
├── docs/
│   ├── architecture.puml
│   └── architecture.drawio
├── src/main/java/nz/co/ksktech/congresstrades/
│   ├── api/
│   │   ├── TradeResource.java          # GET /api/v1/trades (filter by member, ticker, date, type)
│   │   ├── MemberResource.java         # GET /api/v1/members, /members/{name}/trades
│   │   ├── SignalResource.java         # GET /api/v1/signals (clusters, outliers)
│   │   ├── DigestResource.java         # GET /api/v1/digest/daily (LLM-generated)
│   │   ├── dto/
│   │   └── exception/ (GlobalExceptionMapper, custom exceptions)
│   ├── client/
│   │   ├── FinnhubClient.java          # @RegisterRestClient — trades + quotes
│   │   ├── AnthropicClient.java        # @RegisterRestClient — Claude messages API
│   │   └── dto/ (records matching REAL Finnhub + Anthropic JSON shapes)
│   ├── domain/
│   │   ├── Trade.java                  # entity
│   │   ├── Member.java                 # entity
│   │   ├── Signal.java                 # entity (detected pattern)
│   │   └── enums (TransactionType, SignalType, Chamber)
│   ├── repository/                     # Panache repositories
│   ├── service/
│   │   ├── TradeIngestionService.java  # fetch from Finnhub, dedupe, persist
│   │   ├── SignalDetectionService.java # cluster/outlier/speed detection (pure code)
│   │   ├── PerformanceTrackingService.java # post-trade return vs baseline
│   │   └── LlmInsightService.java      # build prompt from computed data, call Claude
│   ├── scheduler/
│   │   └── IngestionScheduler.java     # @Scheduled ingestion (configurable cron)
│   └── config/
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
│       ├── V1__create_members.sql
│       ├── V2__create_trades.sql
│       ├── V3__create_signals.sql
│       └── V4__seed_watchlist.sql      # seed a handful of tickers/members to track
└── src/test/java/nz/co/ksktech/congresstrades/
    ├── api/TradeResourceTest.java
    ├── service/SignalDetectionServiceTest.java
    └── service/LlmInsightServiceTest.java
```

## Functional requirements

### Persistence (PostgreSQL via docker-compose)
- Entities use Panache (PanacheEntityBase, public fields convention).
- `Member`: id, fullName (unique), chamber (enum HOUSE/SENATE), party, state.
- `Trade`: id, member (ManyToOne), ticker, assetDescription, transactionType
  (PURCHASE/SALE/EXCHANGE), amountRangeLow, amountRangeHigh (BigDecimal),
  transactionDate, disclosureDate, daysToDisclose (computed), sourceFilingId (unique
  for idempotent ingestion).
- `Signal`: id, signalType (CLUSTER/OUTLIER/LATE_DISCLOSURE/SECTOR_CONCENTRATION),
  ticker, description, detectedAt, relatedTradeIds, score.
- Flyway migrations V1–V3 create tables with sensible indexes (ticker, member,
  transactionDate). V4 seeds a watchlist of ~10 well-known tickers.
- Hibernate generation = validate.
- Ingestion must be idempotent: unique constraint on sourceFilingId, upsert logic.

### External clients
- `FinnhubClient` @RegisterRestClient(configKey="finnhub-api"):
    - getCongressionalTrades(symbol, token) and getQuote(symbol, token).
    - token passed as @QueryParam (per Finnhub's real auth model).
    - @Retry, @Timeout(5000), @CircuitBreaker on each method.
- `AnthropicClient` @RegisterRestClient(configKey="anthropic-api"):
    - POST /v1/messages.
    - Headers via a ClientHeadersFactory: x-api-key=${ANTHROPIC_API_KEY},
      anthropic-version=2023-06-01, content-type=application/json.
    - Request/response DTOs matching the real Anthropic messages API shape
      (model, max_tokens, messages[]; response content[] blocks).

### Signal detection (PURE CODE — no LLM)
Implement in SignalDetectionService, deterministic and unit-tested:
- CLUSTER: 3+ distinct members buying the same ticker within a 14-day window.
- OUTLIER: a member whose trade amount is >3x their historical median.
- LATE_DISCLOSURE: daysToDisclose > 40 (near the 45-day legal limit).
- SECTOR_CONCENTRATION: (simplified) member with 3+ trades in the same ticker in 30 days.
  Persist detected signals; expose via SignalResource.

### LLM digest (Claude — narrative only)
- LlmInsightService takes the day's computed trades + detected signals and builds
  a structured text prompt, then calls AnthropicClient to produce a readable
  morning-briefing-style summary.
- The system prompt MUST instruct Claude to summarise and contextualise only,
  to avoid buy/sell recommendations, and to note the data's limitations.
- DigestResource GET /api/v1/digest/daily returns the generated narrative plus
  the structured signals it was based on, and the disclaimer.
- Cache the digest (quarkus-cache) so repeated calls in a day don't re-bill the API.

### Scheduler
- IngestionScheduler with @Scheduled, cron configurable via property
  (default every 30 min in prod, but DISABLED by default in %dev so local runs
  don't auto-hit the API — provide a manual POST /api/v1/admin/ingest trigger instead).

### Configuration
- application.properties: datasource (docker-compose Postgres), Hibernate, Flyway,
  both rest-client URLs/timeouts, scheduler cron, cache config, env-var-driven keys.
- %dev: SQL logging on, scheduler off, permissive CORS.
- %test: Testcontainers Postgres; external clients mocked (no real API calls in tests).
- quarkus-smallrye-health with DB readiness check.
- quarkus-smallrye-openapi + Swagger UI.

### Testing
- TradeResourceTest: @QuarkusTest + REST Assured, CRUD/filter paths, Testcontainers PG.
- SignalDetectionServiceTest: pure unit tests for each signal rule with crafted data.
- LlmInsightServiceTest: WireMock-mock the Anthropic API; assert prompt is built
  correctly and response parsed; NEVER call the real API in tests.
- All external HTTP mocked via WireMock (@QuarkusTestResource).

## Maven build
- quarkus-bom; extensions: quarkus-rest-jackson, quarkus-hibernate-orm-panache,
  quarkus-jdbc-postgresql, quarkus-flyway, quarkus-hibernate-validator,
  quarkus-rest-client-jackson, quarkus-smallrye-fault-tolerance, quarkus-scheduler,
  quarkus-cache, quarkus-smallrye-openapi, quarkus-smallrye-health.
- Test: quarkus-junit5, rest-assured, testcontainers-postgresql, wiremock-standalone.
- Java 17, UTF-8, Surefire + Failsafe, standard native profile. Generate mvnw wrapper.

## docker-compose.yml
- postgres:16 on 5432 (db congress, user congress_app, env-var password w/ default),
  named volume, healthcheck. Optional pgadmin on 5050.

## .env.example
Document FINNHUB_API_KEY and ANTHROPIC_API_KEY with placeholder values and a
comment on where to obtain each (finnhub.io free signup; console.anthropic.com).

## CLAUDE.md
Project overview + the honest "research tool not financial advice" framing.
Run commands, key URLs, architecture/layering conventions, Panache + @RestClient
rules, how secrets are handled (env vars), how to add a new signal rule, how to
add a new external client, common pitfalls.

## .github/copilot-instructions.md
Imperative Quarkus rules with Spring→Quarkus guardrails (no @Autowired,
@RestController, RestTemplate, Spring Data). Reference FinnhubClient as the
canonical external-client example and SignalDetectionService as the pattern for
pure-code analysis. State the no-financial-advice rule for any digest/LLM code.

## Diagrams (docs/)
1. architecture.puml — PlantUML: scheduler + REST API → service layer
   (ingestion/signal/performance/LLM) → Finnhub, Anthropic, PostgreSQL. Show the
   fault-tolerance ring on external calls and the cache on the digest path.
2. architecture.drawio — same diagram as draw.io-importable mxGraphModel XML.

## Output expectations
- All files complete and compilable — no TODO stubs.
- Verify with: `docker compose up -d && ./mvnw verify` (tests use Testcontainers +
  WireMock, NO real external API calls). Fix any errors until green.
- Final summary: exact commands to run, how to set the two env vars, sample curl
  commands for each endpoint, and how to trigger a manual ingest in dev.