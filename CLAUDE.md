# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workflow (behavioral-sdd)

No code is written without a saved plan in `.plans/`.

```
/bsdd-prd (optional)  →  /bsdd-plan  →  /bsdd-implement <title>  →  /bsdd-ship
```

- For a larger feature: `/bsdd-prd` (grill-me → saves `.prds/`) → `/bsdd-plan`
- For a known task or existing PRD: `/bsdd-plan` directly (feed the PRD file as input)
- Existing `docs/prd/` files are valid, rich input to `/bsdd-plan`
- `docs/tasks/` files are historical (completed phases 01–04) — new sessions use `.plans/`

### api-tests requirement

Every plan **must** include an `## API tests` section listing which `api-tests/*.http` files will be created or updated. Every implementation **must** produce those files as part of the deliverable.

`api-tests/` files use IntelliJ HTTP Client format:
- Steps are numbered and commented (`### Step N — Description`)
- Each step that returns an ID captures it via `> {% client.global.set("varName", response.body.id); %}`
- Use `{{run}}` (set to `String(Date.now())` in the login step) for unique string fields to avoid constraint conflicts on re-runs
- Use `{{$uuid}}` for barcode/unique fields
- End with an `### --- Error cases ---` section covering 4xx responses
- File name follows the feature/flow (e.g., `sale-flow.http`, `store-credit-flow.http`)

### Active skills

| Skill | Purpose |
|---|---|
| `.skills/caveman.md` | Ultra-compressed output (always active) |
| `.skills/karpathy-guidelines.md` | Think before coding, simplicity, surgical changes (always active) |
| `.skills/code-style.md` | Java/Spring naming, Lombok rules, factory methods, DTO patterns |
| `.skills/error-handling.md` | Exception hierarchy, GlobalExceptionHandler, no catch+log in use cases |
| `.skills/testing-strategy.md` | AbstractIT, Testcontainers, write→index→query pattern, naming |
| `.skills/plan-first-development.md` | Plan discipline rules and checklist |
| `.skills/diff-review.md` | Review process and severity labeling |
| `.skills/grill-me.md` | Plan challenge methodology |
| `.skills/edge-case-generation.md` | Systematic edge case discovery |
| `.skills/postgres-explain-analyze.md` | Query analysis with EXPLAIN ANALYZE |
| `.skills/benchmark-execution.md` | Load testing methodology |
| `.skills/database-seeding.md` | Realistic data for perf tests |
| `.skills/optimization-reporting.md` | Optimization report format |

### Plugins

External tool plugins are declared in `.bsdd-plugins.yml` at the project root. Each plugin lives in `.skills/plugins/<name>.md`.

| Plugin | Sub-agent | Purpose | Auto-detection |
|---|---|---|---|
| `sonar` | reviewer | SonarQube static analysis | `sonar-project.properties` present |
| `xlint-removal` | reviewer | `@Deprecated(forRemoval=true)` warnings — Java only | `build.gradle.kts` present |
| `trivy` | reviewer | CVE scan on dependencies | Docker available |

---

## Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "github.io.ddmfuhrmann.outfit.SomeTest"

# Run application
./gradlew bootRun

# Compile without tests
./gradlew assemble
```

## Architecture

Retail management system — modular monolith migrated from a Vaadin legacy app. Full spec in `docs/project-spec.md`.
PRD and task authoring process: `spec/workflow.md`.

**Stack:** Java 21 · Spring Boot 3.x · Spring Modulith · Spring Data JPA · PostgreSQL · Flyway · Spring Security (stateless JWT) · Elasticsearch (Elastic Java Client only) · JUnit 5 + Testcontainers

**Base package:** `github.io.ddmfuhrmann.outfit`

### Module structure

Bounded contexts are plain packages under the base package. No Gradle submodules. Spring Modulith enforces boundaries at test time via `ModularStructureTest` (`modules.verify()`) — this runs on every build and will fail the build if any module imports from another module's sub-packages.

Each module follows clean architecture layers:
```
{module}/
  domain/
    model/        ← entities, value objects, enums (zero Spring deps; AbstractAggregateRoot allowed on aggregates)
    repository/   ← repository interfaces (ports)
    event/        ← domain event records (public API surface of the module)
  application/
    usecase/      ← one class per operation (imperative names: PlaceSaleUseCase)
    dto/          ← input/output DTOs
    listener/     ← @ApplicationModuleListener implementations
  infrastructure/
    persistence/  ← JPA repository implementations
  api/
    rest/         ← controllers and mappers
```

**Planned bounded contexts** (delivery is phased — most don't exist yet):

| Module | Responsibility | Store |
|---|---|---|
| `shared` | Company, users, auth, cities | PostgreSQL |
| `party` | Customers, suppliers, sellers, addresses | PostgreSQL |
| `catalog` | Products, SKUs, taxes, reference data | PostgreSQL |
| `inventory` | Stock ledger, recounts, balance queries | PostgreSQL |
| `sales` | Orders, consignments, store credit, commissions | PostgreSQL |
| `purchasing` | Supplier purchases, payables, returns | PostgreSQL |
| `finance` | Cash register, receivables | PostgreSQL |
| `query` | Read-side projections via domain events | Elasticsearch |
| `fiscal` | NFC-e / NF-e emission (user-triggered) | PostgreSQL |

### Cross-cutting rules

- **Cross-module communication:** domain events only. No module may import from another module's sub-packages. Only `{module}.domain.event.*` records may be referenced across boundaries. Exception: modules may inject Spring beans from another module's `@NamedInterface` packages (e.g., application services from `shared`). Repositories are never part of a module's public API — always expose an application service (use case) instead.
- **Domain events:** Java `record`s, past-tense names (`SaleConfirmed` not `SaleConfirmEvent`), registered via `registerEvent()` inside aggregate root methods — never published from use cases via `ApplicationEventPublisher`. Events carry the producing module's **own state** — no DB queries in listeners or indexing use cases for data the aggregate owns. IDs are TSID-generated in the base class constructor before persistence, so `getId()` is non-null at any point — events can be registered directly in factory methods and domain methods, including for child entities. For aggregates consumed by the `query` module: (a) own child-entity data is carried as snapshot records (e.g., `SaleItemSnapshot` inside `SaleConfirmed`); (b) cross-module references (e.g., `customerId`, `productId`) are carried as plain IDs — names and descriptions are resolved by the `query` module from existing ES indices at indexing time (see enrichment rule). Simple reference-data events (e.g., `BrandRenamed`) do not need a snapshot.
- **Listeners:** annotated `@ApplicationModuleListener`, named `{Action}Listener`, never `@Transactional` — they delegate to a use case that owns the transaction.
- **Read side — query module is exclusive:** The `query` module is the exclusive owner of all list and detail GET endpoints. Owning modules (sales, party, inventory, etc.) expose only write endpoints (POST / PUT / DELETE). They never implement GET list or GET detail endpoints backed by PostgreSQL — those always live in `query`. Synchronous indexing (`SyncTaskExecutor`) ensures the Elasticsearch document is present before the HTTP response returns, so redirecting the frontend to a query endpoint immediately after a write is always safe.
- **Elasticsearch:** used exclusively in the `query` module via `co.elastic.clients.elasticsearch.ElasticsearchClient`. Spring Data Elasticsearch is not used anywhere. The `query` module never writes to PostgreSQL and never publishes events.
- **Query module enrichment — ES-to-ES only:** When indexing a document, the `query` module enriches cross-module reference data (customer names, seller names, product descriptions, brand, color, size) by querying **existing Elasticsearch indices** — it never queries PostgreSQL and never calls owning module services directly. Reference the index name constants from `ElasticsearchIndexInitializer` (e.g., `INDEX_PARTIES`, `INDEX_PRODUCTS`). Domain events carry cross-module references as plain IDs (`customerId`, `productId`); names and descriptions are resolved from ES at indexing time. The `query` module may also call application services from `shared` (via `@NamedInterface`) to enrich with shared reference data (e.g., city names) — this is correct aggregation, not coupling.
- **ES index names and exceptions:** Elasticsearch index names and mapped field names are declared as `static final String` constants in `ElasticsearchIndexInitializer` — always reference those constants instead of hardcoding strings in use cases or tests. Infrastructure failures during index initialisation must be wrapped as `IndexingException` (not raw `RuntimeException`); query-time failures must be wrapped as `QueryException`. Both extend `ElasticsearchException`.
- **ES replication — sync vs async:** replication can be synchronous (read-your-writes) or asynchronous (eventual consistency) depending on the entity's needs. The global mode is controlled by the `TaskExecutor` bean in `shared.infrastructure.EventPublicationConfig`. Currently synchronous (`SyncTaskExecutor`): listeners run in the same request thread after the Postgres commit, before the HTTP response is sent, so a GET immediately after a write always reflects the indexed state.
  - **Global async:** if all ES projections in the system can tolerate eventual consistency, replace `SyncTaskExecutor` with `ThreadPoolTaskExecutor` in `EventPublicationConfig` and add an eventual-consistency note to all query endpoint OpenAPI descriptions.
  - **Per-listener async (preferred when mixing sync and async):** keep `EventPublicationConfig` unchanged. In the module's `infrastructure/` package, declare a dedicated `@Bean("myProjectionExecutor") ThreadPoolTaskExecutor`. On the specific listener method, use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async("myProjectionExecutor")` **instead of** `@ApplicationModuleListener` — `@ApplicationModuleListener` always uses the global executor and cannot be overridden per-listener. Document eventual consistency only on the endpoints served by that async listener.
- **OpenAPI (query endpoints):** must note that the endpoint is served from Elasticsearch. If replication is synchronous, no eventual consistency caveat is needed. If replication is async, add: "Eventually consistent — reflects the state of the index after domain events are processed."
- **Fiscal module:** emission is user-initiated via REST (never triggered automatically by events). After successful emission it publishes `NfceEmitted` / `NfeEmitted`. All SEFAZ communication is encapsulated in `fiscal.infrastructure.javaNfe`.
- **`@Transactional`:** only on use case or service methods — never on domain entities.
- **Monetary values:** `BigDecimal` in Java, `NUMERIC(15,2)` in PostgreSQL — no `float` or `double`.
- **Enums:** stored as `VARCHAR`, not ordinal integers.
- **Timestamps:** UTC in Java, `TIMESTAMPTZ` in PostgreSQL.
- **Clock injection:** never call `Instant.now()` directly in production code. Inject `java.time.Clock` and call `Instant.now(clock)`. The `Clock` bean is registered in `shared/infrastructure/ClockConfig` (`@Profile("!test")`). Tests use `FixedClockConfig` (imported via `AbstractIT`) which fixes the clock at `2025-06-04T00:00:00Z`, making all timestamp-dependent assertions deterministic. **Exception:** `JwtService` — JJWT 0.12's parser validates token expiry against the system clock internally; injecting a fixed past clock would expire all tokens. Domain methods that need the current time receive `Instant now` as a parameter (e.g., `StockRecount.close(Instant now)`) — the use case passes `Instant.now(clock)` at the call site.
- **Pagination:** all list responses use the standard `PageResponse<T>` wrapper.
- **List access:** use `getFirst()` instead of `get(0)` when accessing the first element of a list (Java 21+).
- **OpenAPI:** spec served at `/docs` via SpringDoc.
- **Schema changes:** Flyway only — no `spring.jpa.hibernate.ddl-auto` in production.
- **Auth:** stateless JWT; all endpoints secured except `/auth/**`.
- **Optimistic locking** required on financial aggregates (sale, receivable, payable).
- **Entity base classes:** aggregate roots extend `BaseAggregate<T>` (inherits audit fields + `registerEvent()`). Plain entities that are not aggregate roots extend `BaseEntity`. Never use `AbstractAggregateRoot` directly.
- **Domain invariants:** throw `IllegalArgumentException` for invalid input state (missing or malformed field); throw `IllegalStateException` for invalid operation sequences (e.g. deactivating an already-inactive user). `GlobalExceptionHandler` maps them to `400 Bad Request` and `422 Unprocessable Entity` respectively — domain classes must never import Spring or HTTP types.
- **Lombok:** `@Getter` is the only annotation allowed on entities. `@Setter`, `@Data`, `@Builder`, `@AllArgsConstructor`, and `@RequiredArgsConstructor` are prohibited — they bypass domain logic or expose public mutation. DTOs are Java records and need no Lombok. When a factory method has many parameters, use a hand-written static inner `Builder` class instead of `@Builder` — `build()` delegates to the same validation logic, keeping all invariants in one place.
- **Entity instantiation:** entities are never instantiated with `new` from outside the class. Aggregate roots expose a `static create(...)` factory method. Child entities are created exclusively via aggregate root domain methods. All entities have a `protected` no-arg constructor for JPA only.
- **Value objects:** domain-specific types with validation (e.g. `Cpf`, `Cnpj`) are implemented as `@Embeddable` classes. They have a `protected` no-arg constructor for JPA, a private constructor, and a `static of(...)` factory that validates and constructs. JPA flattens the embedded columns into the owning table — no join table. Use `@Embedded` on the owning field. Value objects do not extend `BaseEntity` or `BaseAggregate`.
- **Boolean fields:** use plain names (`customer`, `supplier`, `active`) — never `is`-prefixed field names (`isCustomer`). Lombok's `@Getter` on a `boolean` field already generates the correct Java Bean accessor (`isCustomer()`) automatically.
- **No public setters:** mutation happens exclusively through named domain behavior methods (`deactivate()`, `updateProfile()`, `confirm()`). PUT use cases load the entity and call its domain method — JPA dirty checking handles persistence within `@Transactional`. **Exception:** use cases that need to publish domain events must call `repository.save(aggregate)` explicitly so Spring Data reads `@DomainEvents`.
- **DTO mapping:** DTOs own the mapping in both directions. Response DTOs have a `static from(Entity)` factory method. Request DTO fields are passed individually to the entity factory or domain method by the use case. No mapping framework (MapStruct, ModelMapper) is used.
- **Logging:** `@Slf4j` (Lombok) on use cases — never on entities. Write operations log `INFO` after the operation succeeds. Failed authentication logs `WARN`. Read-only use cases (Get/List) log nothing at `INFO` — high-frequency calls add noise. Never log sensitive data (passwords, tokens, PII).
- **Method length:** keep methods short and single-purpose. If a method grows beyond ~15 lines, extract private methods with names that describe *what* each step does. Public `execute()` methods in use cases should read as a high-level sequence of named steps, not inline implementation.
- **Error logging:** `GlobalExceptionHandler` is the sole logging point for errors — use cases never catch and log exceptions. By severity: 5xx and infrastructure failures → `log.error` with full stack trace; constraint violations (409), domain argument violations (400 from `IllegalArgumentException`), and invalid operation sequences (422 from `IllegalStateException`) → `log.warn` without stack trace; expected 4xx client errors → no logging. Empty catch blocks are forbidden — use at minimum `log.warn(e.getMessage())`.
- **Test date fixtures:** use `Month.JANUARY` (not `1`) in `LocalDate.of()` calls — e.g. `LocalDate.of(2025, Month.JANUARY, 10)`. Avoids Sonar's magic-number warning and makes month intent explicit.
- **Test event filtering:** filter domain events with `EventType.class::isInstance` (not `e -> e instanceof EventType`) — e.g. `.filter(PurchaseConfirmed.class::isInstance)`.
