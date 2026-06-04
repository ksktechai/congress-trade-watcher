# Copilot / AI agent instructions — Congress Trade Watcher

This is a **Quarkus 3 (Java 17)** app. Generate **idiomatic Quarkus**, never Spring.
Package root: `nz.co.ksktech.congresstrades`.

## Honesty rule (applies to all digest/LLM code)

This is a **research tool, NOT financial advice**. Any code that builds prompts or
handles LLM output MUST keep these guarantees: the model only **summarises and
contextualises** already-computed data; it must **never** recommend buy/sell/hold,
predict prices, or invent trades/signals. Keep the constraints in
`LlmInsightService.SYSTEM_PROMPT` and always include
`Disclaimers.NOT_FINANCIAL_ADVICE` in digest output.

## Spring → Quarkus guardrails (do NOT use these)

| ❌ Spring                         | ✅ Quarkus                                              |
|----------------------------------|--------------------------------------------------------|
| `@Autowired`                     | constructor injection, or `@Inject`                    |
| `@RestController` / `@GetMapping`| JAX-RS `@Path` + `@GET`/`@POST` + `@QueryParam`         |
| `@Service` / `@Component`        | `@ApplicationScoped`                                    |
| `RestTemplate` / `WebClient`     | `@RegisterRestClient` typed interface                  |
| Spring Data `JpaRepository`      | Panache `PanacheRepository`                             |
| `application.yml` Spring keys    | `application.properties` with `quarkus.*` keys          |
| `@Value`                         | `@ConfigProperty` or `@ConfigMapping`                  |
| `@Transactional` (Spring)        | `jakarta.transaction.Transactional`                    |
| `@Scheduled` (Spring)            | `io.quarkus.scheduler.Scheduled`                       |

## Imperative rules

- **Entities**: extend `PanacheEntityBase`, **public fields**, explicit
  `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;`.
  No getters/setters. camelCase fields → snake_case columns.
- **Persistence access**: use the repositories in `repository/`
  (`PanacheRepository`), not active-record calls on entities.
- **Schema**: Hibernate is `validate`-only. Flyway owns the schema — add a new
  `src/main/resources/db/migration/V*__*.sql`; never edit an applied one and never
  enable schema generation.
- **External clients**: model after `FinnhubClient` — a
  `@RegisterRestClient(configKey=...)` interface with `@Timeout` + `@Retry` +
  `@CircuitBreaker` on every method, DTO **records** matching the real JSON
  (`@JsonIgnoreProperties(ignoreUnknown = true)`). Base URL lives in
  `application.properties`. Inject with `@Inject @RestClient`.
- **Secrets**: env vars only (`FINNHUB_API_KEY`, `ANTHROPIC_API_KEY`), referenced
  as `${...}` in properties. Never hard-code or commit a key.
- **Pure analysis**: new analytics go in `SignalDetectionService` as pure,
  deterministic methods over `List<Trade>` — **no LLM, no external calls** — and
  must be unit-tested. This is the canonical pattern for app-computed signals.
- **REST resources**: thin. Map entities to DTOs (`api/dto/`), delegate to
  services, let `GlobalExceptionMapper` shape errors. Don't return entities.
- **Tests**: `@QuarkusTest` + REST Assured for endpoints (Testcontainers Postgres
  via Dev Services); WireMock for all external HTTP. Never call a real external
  API in a test. Keep pure-logic tests as plain JUnit.

## Canonical references in this repo

- External client: `client/FinnhubClient.java`
- Pure-code analysis: `service/SignalDetectionService.java`
- LLM boundary + disclaimer: `service/LlmInsightService.java`, `config/Disclaimers.java`
- Idempotent ingestion: `service/TradeIngestionService.java`
