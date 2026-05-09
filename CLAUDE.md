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

- **Cross-module communication:** domain events only. No module may import from another module's sub-packages. Only `{module}.domain.event.*` records may be referenced across boundaries.
- **Domain events:** Java `record`s, past-tense names (`SaleConfirmed` not `SaleConfirmEvent`), published by aggregate roots via `registerEvent()`.
- **Listeners:** annotated `@ApplicationModuleListener`, named `{Action}Listener`, never `@Transactional` — they delegate to a use case that owns the transaction.
- **Elasticsearch:** used exclusively in the `query` module via `co.elastic.clients.elasticsearch.ElasticsearchClient`. Spring Data Elasticsearch is not used anywhere. The `query` module never writes to PostgreSQL and never publishes events.
- **Fiscal module:** emission is user-initiated via REST (never triggered automatically by events). After successful emission it publishes `NfceEmitted` / `NfeEmitted`. All SEFAZ communication is encapsulated in `fiscal.infrastructure.javaNfe`.
- **`@Transactional`:** only on use case or service methods — never on domain entities.
- **Monetary values:** `BigDecimal` in Java, `NUMERIC(15,2)` in PostgreSQL — no `float` or `double`.
- **Enums:** stored as `VARCHAR`, not ordinal integers.
- **Timestamps:** UTC in Java, `TIMESTAMPTZ` in PostgreSQL.
- **Pagination:** all list responses use the standard `PageResponse<T>` wrapper.
- **OpenAPI:** spec served at `/docs` via SpringDoc. Query endpoints must document eventual consistency.
- **Schema changes:** Flyway only — no `spring.jpa.hibernate.ddl-auto` in production.
- **Auth:** stateless JWT; all endpoints secured except `/auth/**`.
- **Optimistic locking** required on financial aggregates (sale, receivable, payable).
