# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- **Domain events:** Java `record`s, past-tense names (`SaleConfirmed` not `SaleConfirmEvent`), registered via `registerEvent()` inside aggregate root methods — never published from use cases via `ApplicationEventPublisher`. Events carry the producing module's **own state** — no DB queries in listeners or indexing use cases for data the aggregate owns. IDs are TSID-generated in the base class constructor before persistence, so `getId()` is non-null at any point — events can be registered directly in factory methods and domain methods, including for child entities. For aggregates consumed by the `query` module, events carry a `{Aggregate}Snapshot` record built from the aggregate's current state via a private `toSnapshot()` method — child entities get nested snapshot records (e.g., `ProductSkuSnapshot` inside `ProductSnapshot`). Simple reference-data events (e.g., `BrandRenamed`) do not need a snapshot.
- **Listeners:** annotated `@ApplicationModuleListener`, named `{Action}Listener`, never `@Transactional` — they delegate to a use case that owns the transaction.
- **Elasticsearch:** used exclusively in the `query` module via `co.elastic.clients.elasticsearch.ElasticsearchClient`. Spring Data Elasticsearch is not used anywhere. The `query` module never writes to PostgreSQL and never publishes events. The `query` module may call application services from `shared` (via `@NamedInterface`) to enrich documents with shared reference data (e.g., city names) — this is correct aggregation, not coupling.
- **ES index names and exceptions:** Elasticsearch index names and mapped field names are declared as `static final String` constants in `ElasticsearchIndexInitializer` — always reference those constants instead of hardcoding strings in use cases or tests. Infrastructure failures during index initialisation must be wrapped as `IndexingException` (not raw `RuntimeException`); query-time failures must be wrapped as `QueryException`. Both extend `ElasticsearchException`.
- **ES replication — sync vs async:** replication can be synchronous (read-your-writes) or asynchronous (eventual consistency) depending on the entity's needs. The mode is controlled by the `TaskExecutor` bean in `shared.infrastructure.EventPublicationConfig`. Currently synchronous (`SyncTaskExecutor`): listeners run in the same request thread after the Postgres commit, before the HTTP response is sent, so a GET immediately after a write always reflects the indexed state. If a future module requires async replication (e.g., high-throughput indexing tolerant of lag), replace `SyncTaskExecutor` with `ThreadPoolTaskExecutor` and document eventual consistency in those OpenAPI endpoints.
- **OpenAPI (query endpoints):** must note that the endpoint is served from Elasticsearch. If replication is synchronous, no eventual consistency caveat is needed. If replication is async, add: "Eventually consistent — reflects the state of the index after domain events are processed."
- **Fiscal module:** emission is user-initiated via REST (never triggered automatically by events). After successful emission it publishes `NfceEmitted` / `NfeEmitted`. All SEFAZ communication is encapsulated in `fiscal.infrastructure.javaNfe`.
- **`@Transactional`:** only on use case or service methods — never on domain entities.
- **Monetary values:** `BigDecimal` in Java, `NUMERIC(15,2)` in PostgreSQL — no `float` or `double`.
- **Enums:** stored as `VARCHAR`, not ordinal integers.
- **Timestamps:** UTC in Java, `TIMESTAMPTZ` in PostgreSQL.
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
- **Error logging:** `GlobalExceptionHandler` is the sole logging point for errors — use cases never catch and log exceptions. By severity: 5xx and infrastructure failures → `log.error` with full stack trace; constraint violations (409), domain argument violations (400 from `IllegalArgumentException`), and invalid operation sequences (422 from `IllegalStateException`) → `log.warn` without stack trace; expected 4xx client errors → no logging. Empty catch blocks are forbidden — use at minimum `log.warn(e.getMessage())`.
