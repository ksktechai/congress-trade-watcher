# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository.

## What this is

**Congress Trade Watcher** ingests US Congressional stock-trade disclosures
(STOCK Act filings, via Finnhub), persists/analyses them in PostgreSQL, detects
rule-based patterns in pure code, and generates a plain-English daily digest with
the Anthropic Claude API.

### Non-negotiable framing: research tool, NOT financial advice

Congressional trades are disclosed up to **45 days late**, amounts are **ranges**
not exact figures, and following them does **not** reliably beat the market.

- The app surfaces **patterns for research only**. It must never present output as
  financial advice or a buy/sell signal.
- The LLM is used **only to summarise and contextualise** data the app has already
  computed. It must never select tickers, rank "best" trades, or predict prices.
  `LlmInsightService.SYSTEM_PROMPT` enforces this — keep those constraints if you
  touch it.
- The disclaimer (`config/Disclaimers.NOT_FINANCIAL_ADVICE`) appears in the digest
  API output and the README. Keep it there.

## Tech stack

Quarkus 3.15 · Java 17 · Maven (`./mvnw`) · PostgreSQL 16 · Hibernate ORM with
Panache · Flyway · MicroProfile REST Client + Fault Tolerance · Quarkus Scheduler
· Quarkus Cache · SmallRye OpenAPI/Health · JUnit 5 + REST Assured + WireMock +
Testcontainers.

Package root: `nz.co.ksktech.congresstrades`.

## Common commands

```bash
docker compose up -d            # Postgres (required for dev/prod run)
./mvnw quarkus:dev              # dev mode, live reload (scheduler OFF in dev)
./mvnw verify                   # tests (Testcontainers + WireMock) + package
./mvnw -DskipTests package      # build only

curl -X POST localhost:8080/api/v1/admin/ingest        # manual ingest (dev)
curl localhost:8080/api/v1/digest/daily                # LLM digest
```

Key URLs: Swagger UI `/q/swagger-ui`, OpenAPI `/q/openapi`, Health `/q/health`.

## Secrets

API keys are **environment variables only**: `FINNHUB_API_KEY`, plus the LLM key
for the active provider (`GEMINI_API_KEY` by default, or `ANTHROPIC_API_KEY`).
They are referenced in `application.properties` as `${FINNHUB_API_KEY}` etc.
(surfaced through the `watcher.*` `@ConfigMapping`, `AppConfig`). All keys are
modelled as `Optional<String>` so the app boots even when unset — a key is only
needed when that remote call is actually made. **Never** hard-code a key or put
one in a properties file or commit `.env`.

## Ingestion sources (pluggable)

Trade data has two sources, chosen by `ingestion.source` (`INGESTION_SOURCE` env)
and the `?source=` admin param:
- **`congress`** (default, free, no key) — `CongressDataIngestionService` +
  `CongressDataClient`, pulling the Congress Trading Monitor open dataset
  (congress.kadoa.com, MIT). Real STOCK Act filings with a stable `id` per filing.
- **`finnhub`** (premium key) — `TradeIngestionService` + `FinnhubClient`.

Both normalise raw records to `TradeUpsertService.NormalizedTrade` and call the
single idempotent `TradeUpsertService.upsert` (keyed on `sourceFilingId`), which
also defensively truncates strings to column widths — real PDF-parsed data can be
very long/noisy. To add a source: new client + DTOs + an ingestion service that
builds `NormalizedTrade`s; wire it into `AdminResource` and `IngestionScheduler`.

## Logging / call tracing

The call sequence is logged at INFO so you can follow it in the console:
`API REQUEST/RESPONSE` (`ApiAccessLoggingFilter`, skips `/q/*`),
`INGEST REQUEST/RESPONSE` (per ticker), `DIGEST step 1..3/3`, and
`LLM REQUEST/RESPONSE` (provider, model, char counts, token usage, timing,
finishReason). In `%dev` the app package is DEBUG, which also prints the **full
LLM prompts and response bodies**. **Never log API keys** — that's why we use
app-level logging instead of Quarkus's blanket REST-client logging (which would
dump the `x-api-key`/`X-goog-api-key` headers and Finnhub's `?token=`).

## LLM provider (narration only)

The digest narrator is pluggable via `watcher.llm.provider` (`LLM_PROVIDER` env),
default `gemini`. To add/choose a provider: implement `LlmProvider`
(`@ApplicationScoped`, with an `id()`), back it with a `@RegisterRestClient`
client; `LlmInsightService` selects the bean whose `id()` matches the config.
`GeminiLlmProvider` and `AnthropicLlmProvider` are the two examples. Whatever the
provider, the no-recommendations system prompt and disclaimer rules still apply.

## Architecture & layering (respect these boundaries)

```
api  ──►  service  ──►  repository  ──►  domain
                  └──►  client (Finnhub, Anthropic)
```

- **api/** — JAX-RS resources, DTOs, `GlobalExceptionMapper`. No business logic;
  resources map to/from DTOs and delegate to services.
- **service/** — all logic. `SignalDetectionService` is **pure** (no DB, no LLM,
  no clock surprises) so it is deterministic and unit-tested.
- **client/** — `@RegisterRestClient` typed clients only.
- **repository/** — Panache repositories (the project uses the *repository*
  pattern, not active-record on the entity).
- **domain/** — Panache entities with **public fields** + enums.

## Conventions & rules

### Panache
- Entities extend `PanacheEntityBase` with an explicit
  `@Id @GeneratedValue(strategy = IDENTITY) public Long id;` so the schema matches
  Flyway's `BIGINT GENERATED BY DEFAULT AS IDENTITY` under Hibernate `validate`.
- Public fields, no getters/setters. Access via repositories.
- camelCase fields map to snake_case columns
  (`CamelCaseToUnderscoresNamingStrategy`).

### Schema is owned by Flyway
- `quarkus.hibernate-orm.database.generation=validate`. **Never** let Hibernate
  generate the schema. Any entity change needs a new `V*__*.sql` migration; never
  edit an applied migration — add the next version.

### REST clients (see `FinnhubClient` — the canonical example)
- One `@RegisterRestClient(configKey=...)` interface per provider, base URL in
  `application.properties` (`quarkus.rest-client.<key>.url`).
- Every remote method carries the fault-tolerance ring:
  `@Timeout` + `@Retry` + `@CircuitBreaker`.
- Finnhub passes the token as a `@QueryParam`. Anthropic auth headers come from
  `AnthropicHeadersFactory` (a `ClientHeadersFactory`) reading the key from config.

### Idempotent ingestion
- `Trade.sourceFilingId` is unique. Finnhub has no stable filing id, so
  `TradeIngestionService` derives a deterministic hash; re-ingestion updates
  instead of duplicating. Each ticker is ingested in its own transaction.

## How to add a new signal rule

1. Add a value to `domain/enums/SignalType`.
2. Add a `detectXxx(List<Trade>)` method to `SignalDetectionService` — **pure
   code**, no injection of clients/repos beyond config. Add it to `detectAll`.
3. Make thresholds configurable via `AppConfig.Signals` (with `@WithDefault`).
4. Add unit tests to `SignalDetectionServiceTest` (one happy + one negative case).
5. Signals persist via `SignalService.detectAndPersist`; the API exposes them
   automatically through `SignalResource`.

## How to add a new external client

1. Create a `@RegisterRestClient(configKey="my-api")` interface in `client/`,
   with DTO records under `client/dto/` that match the **real** JSON shape
   (`@JsonIgnoreProperties(ignoreUnknown=true)` on responses).
2. Add `quarkus.rest-client.my-api.url` + timeouts to `application.properties`.
3. Put `@Timeout`/`@Retry`/`@CircuitBreaker` on each method.
4. Inject with `@Inject @RestClient MyApi client;`.
5. In tests, stub it in `WireMockTestResource` and override
   `quarkus.rest-client.my-api.url` to the WireMock base URL. Never call the real
   API in a test.

## Testing rules

- `%test` uses Quarkus Dev Services (Testcontainers Postgres) — no DB config needed.
- All external HTTP is mocked with WireMock via `WireMockTestResource`.
- Keep `SignalDetectionServiceTest` a plain JUnit test (no `@QuarkusTest`).
- Never put a real API key in a test or commit one.

## Common pitfalls

- **Don't** switch the id strategy away from `IDENTITY` without updating every
  migration — `validate` will fail.
- **Don't** add a property-file secret. Env vars only.
- **Don't** let the LLM make recommendations or invent data — narration only.
- The scheduler is intentionally `off` in dev/test; use `POST /api/v1/admin/ingest`.
- On a JDK newer than the extensions support (e.g. 25), the build needs
  `-Dnet.bytebuddy.experimental=true`; it is already wired in `.mvn/jvm.config`
  and the Surefire/Failsafe config. The supported target is **Java 17**.
