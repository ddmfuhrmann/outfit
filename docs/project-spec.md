# Project specification — retail management system

> Migration from Vaadin legacy monolith to Spring Boot 21 + modern frontend.
> Also serves as a study project for clean architecture and DDD patterns.

---

## 1. Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 (records, sealed classes, pattern matching) |
| Framework | Spring Boot 3.x |
| Persistence | Spring Data JPA + Hibernate / PostgreSQL |
| Migrations | Flyway |
| Build | Gradle (single project, Kotlin DSL) |
| Auth | Spring Security + JWT (stateless) |
| Read store | Elasticsearch + Elasticsearch Java Client (co.elastic.clients) |
| Module boundaries | Spring Modulith |
| Testing | JUnit 5 + Testcontainers + AssertJ |
| Lombok | Getter generation on entities (`@Getter` only) |
| Docs | SpringDoc OpenAPI 3 |
| Frontend | To be defined (separate repo) |
| Containerization | Docker + Docker Compose |

---

## 2. Architecture — modular monolith

### Decision rationale

Microservices were evaluated and ruled out for the following reasons:

- The domain is a single retail store; there is no scale or team justification for distributed services.
- Operational overhead (service discovery, distributed tracing, eventual consistency) would consume time that should go toward the domain model.
- The project also serves as a study reference — a well-structured monolith teaches domain modeling more effectively than infrastructure plumbing.
- Modules will be designed with clean boundaries so that extraction to services is possible in the future if needed.

**Chosen pattern: single-project monolith with package-level isolation per bounded context, enforced by Spring Modulith.** No Gradle submodules — boundaries are verified at test time by Modulith's module structure tests, which replace manual ArchUnit rules for cross-context dependencies.

---

## 3. Package structure

Single Gradle project. All source under `src/main/java/com/retailms`. Bounded contexts are plain packages — no submodules, no separate `build.gradle` files.

```
src/
└── main/
    └── java/
        └── com/retailms/
            │
            ├── config/
            │   ├── domain/
            │   │   └── model/          ← User, Company, Token, Permission
            │   ├── application/
            │   │   └── usecase/        ← CreateUser, UpdateCompany, ChangePassword...
            │   ├── infrastructure/
            │   │   └── persistence/    ← JPA repos, UserDetailsService, JWT filter
            │   └── api/
            │       └── rest/           ← /auth/**, /users/**, /company
            │
            ├── party/
            │   ├── domain/
            │   │   ├── model/          ← entities, value objects, enums
            │   │   └── repository/     ← repository interfaces (ports)
            │   ├── application/
            │   │   ├── usecase/        ← one class per use case
            │   │   └── dto/            ← input/output DTOs
            │   ├── infrastructure/
            │   │   └── persistence/    ← JPA mappings, repository impls
            │   └── api/
            │       └── rest/           ← controllers, mappers
            │
            ├── catalog/                ← same internal structure
            ├── inventory/
            ├── sales/
            ├── purchasing/
            ├── finance/
            ├── query/
            │   ├── domain/
            │   │   └── model/          ← read models (projections)
            │   ├── application/
            │   │   ├── listener/       ← consumes domain events, builds projections
            │   │   └── queryservice/   ← query services (by period, seller, etc.)
            │   ├── infrastructure/
            │   │   └── elasticsearch/  ← Elastic Java client, index mappings
            │   └── api/
            │       └── rest/           ← GET-only endpoints (search, analytics)
            │
            ├── fiscal/
            │   ├── domain/
            │   │   ├── model/          ← FiscalDocument, emission status, enums
            │   │   └── event/          ← NfceEmitted, NfeEmitted
            │   ├── application/
            │   │   └── usecase/        ← EmitNfceUseCase, EmitNfeUseCase, CancelFiscalDocUseCase
            │   ├── infrastructure/
            │   │   └── javaNfe/        ← Java_NFe integration, cert config, SEFAZ client
            │   └── api/
            │       └── rest/           ← emission, status and reissue endpoints
            │
            └── shared/
                ├── domain/             ← base entity, value types, common enums
                ├── infrastructure/     ← audit config, pagination, exception handling
                └── api/                ← global error handler, page response wrapper
```

**Boundary enforcement** — Spring Modulith detects the bounded contexts automatically from the package structure and verifies boundaries at test time:

```java
// src/test/java/com/retailms/ModularStructureTest.java
@ApplicationModuleTest
class ModularStructureTest {

    @Test
    void verifiesModularStructure(ApplicationModules modules) {
        modules.verify(); // fails if any module accesses another's internals
    }

    @Test
    void documentModules(ApplicationModules modules) {
        new Documenter(modules).writeDocumentation(); // generates diagrams
    }
}
```

The public API of each module is whatever lives directly in the module's root package (e.g. `com.retailms.sales`). Everything under sub-packages (`sales.domain`, `sales.infrastructure`) is internal and cannot be imported by other modules. Domain events are the only sanctioned cross-module communication channel.

### Clean architecture rules (lightweight)

- `domain` has zero Spring dependencies. Plain Java only. Exception: `AbstractAggregateRoot` from Spring Data is allowed on aggregate roots that publish events.
- `application` depends only on `domain`. No JPA, no HTTP.
- `infrastructure` depends on `application` and `domain`. Implements repository ports.
- `api` depends on `application`. Calls use cases only — never repositories directly.
- Cross-module communication happens exclusively via domain events. No module may import from another module's sub-packages.

### Domain events convention

Every domain event is a Java `record`, lives in `{module}.domain.event`, and is named in the past tense:

```
sales/
└── domain/
    ├── model/
    │   └── Sale.java               ← extends AbstractAggregateRoot<Sale>
    └── event/
        ├── SaleConfirmed.java      ← public record (the module's API surface)
        ├── SaleItemReturned.java
        └── ConsignmentClosed.java
```

Events are published by aggregate roots via `registerEvent()` inside domain methods. Listeners in other modules use `@ApplicationModuleListener` and are named `{Action}Listener` in their own `application/listener/` package. Each listener calls a use case — never a repository directly.

**Migration path to async:** replace `@ApplicationModuleListener` with `@KafkaListener` / `@RabbitListener` on the consumer side, and inject a `DomainEventPublisher` port on the producer side. The event records and use case logic remain untouched.

### CQRS — write store vs read store

The system applies a lightweight CQRS separation at the infrastructure level:

- **Write side** — all transactional operations (sales, purchases, stock movements) write exclusively to PostgreSQL via JPA. This is the system of record.
- **Read side** — the `query` module maintains denormalized projections in Elasticsearch. It serves two kinds of frontend reads:
  - **List views** — paginated, filterable screens that display data spanning multiple bounded contexts (e.g. a sales list showing customer name, seller name, products). These cannot be served by a single module's PostgreSQL because cross-module JOINs violate module isolation.
  - **Analytics and reports** — aggregations, dashboards, and time-series queries (sales by seller, revenue by period, stock turnover).

Simple within-module reads that happen immediately after a write (e.g. `GET /sales/{id}` right after `POST /sales`) are served by the owning module's own PostgreSQL — these require strong consistency that an async projection cannot guarantee.

No module other than `query` reads from or writes to Elasticsearch. No module other than `query` exposes list or analytical query endpoints that span bounded contexts. The Elasticsearch index schema is internal to `query.infrastructure.elasticsearch` and is not shared with any other module.

The Elastic Java client (`co.elastic.clients.elasticsearch.ElasticsearchClient`) is instantiated as a Spring `@Bean` in `query.infrastructure` and injected only within that package. Spring Data Elasticsearch is explicitly excluded — all index operations use the fluent API of the official client directly.

---

## 4. Bounded contexts

| Context | Responsibility | Write store | Phase |
|---|---|---|---|
| Shared | Technical kernel — base entity, pagination, audit, value types | PostgreSQL | 1 |
| Config | Users, authentication, permissions, company settings | PostgreSQL | 1 |
| Party | Customers, suppliers, sellers, addresses, contacts | PostgreSQL | 2 |
| Catalog | Products, SKUs, taxes, reference data | PostgreSQL | 2 |
| Inventory | Stock ledger, recounts, balance queries | PostgreSQL | 3 |
| Sales | Orders, consignments, store credit, commissions | PostgreSQL | 4 |
| Purchasing | Supplier purchases, payables, returns | PostgreSQL | 5 |
| Finance | Cash register, receivables | PostgreSQL | 6 |
| Query | Read-side projections fed by domain events | Elasticsearch | 7 |
| Fiscal | NFC-e emission (sales) and NF-e emission (supplier returns) | PostgreSQL | 8 |

The `query` context is **read-only and write-never** from the PostgreSQL perspective. It owns its Elasticsearch indices exclusively and exposes only `GET` endpoints. All other contexts own their PostgreSQL schema exclusively and have no knowledge of Elasticsearch.

---

## 5. General project requirements

### Functional requirements

- [ ] CRUD for all reference data (parties, products, categories, sizes, colors, brands).
- [ ] Full sales flow: quote → sale → payment schedule → stock decrement.
- [ ] Consignment flow: issue → partial return → confirm sold items.
- [ ] Store credit flow: receive items back → issue credit note → apply to future sale.
- [ ] Purchase flow: register purchase → generate payables → record payments.
- [ ] Supplier return flow with NF-e emission.
- [ ] NFC-e automatic emission on sale confirmation.
- [ ] Fiscal document status tracking and reissue on rejection.
- [ ] Stock recount with automatic adjustment entries.
- [ ] Cash register: open, record movements, close with balance report.
- [ ] Receivables management with aging report.
- [ ] Commission calculation per seller with bonus tiers.
- [ ] SAT/CF-e fiscal document integration (FiscalDocReturn).
- [ ] Query: sales by seller, sales by period, revenue summary, stock turnover.

### Non-functional requirements

- [ ] Stateless JWT authentication; all endpoints secured except `/auth/**`.
- [ ] All responses paginated using a standard wrapper (`PageResponse<T>`).
- [ ] Optimistic locking on financial aggregates (sale, receivable, payable).
- [ ] All monetary values stored as `NUMERIC(15,2)`; handled as `BigDecimal` in Java — no `float` or `double`.
- [ ] Timestamps in UTC; stored as `TIMESTAMPTZ` in PostgreSQL.
- [ ] Flyway for all schema changes; no auto DDL in production.
- [ ] Integration tests use Testcontainers with a real PostgreSQL instance and an Elasticsearch container.
- [ ] OpenAPI spec auto-generated and served at `/docs`.
- [ ] Structured logging (JSON) in production profile.
- [ ] Query module listeners run synchronously after commit by default (`@ApplicationModuleListener` / `AFTER_COMMIT`) — Elasticsearch is consistent with PostgreSQL before the HTTP response returns. Eventual consistency only applies if async event publication is explicitly enabled in the future.

---

## 6. Phased delivery plan

### Phase 1 — foundation
- [ ] Spring Boot project scaffolding (Gradle, Kotlin DSL, single project).
- [ ] Spring Modulith dependency configured; `ModularStructureTest` passing.
- [ ] Docker Compose with PostgreSQL and Elasticsearch.
- [ ] Flyway base migration (V1): shared schema (cities) and config schema (users, company, tokens).
- [ ] `config` module: JWT authentication, user management, company settings.
- [ ] `shared` kernel: base entity, audit fields, pagination, global error handler.
- [ ] All endpoints secured via Spring Security configured in `config.infrastructure`.

### Phase 2 — reference data
- [ ] Party module (domain → infra → api).
- [ ] Product catalog module (domain → infra → api).
- [ ] Seed data migration for cities, sizes, colors, brands, categories.

### Phase 3 — inventory
- [ ] Inventory module with ledger pattern.
- [ ] Stock balance query service.
- [ ] Integration tests for stock movements.

### Phase 4 — sales core
- [ ] Sales bounded context (domain → infra → api).
- [ ] Domain events wiring: sales publishes, inventory and finance listen.
- [ ] Commission calculation domain service.
- [ ] Module isolation tests (`@ApplicationModuleTest`).
- [ ] Integration tests for full sale flow including cross-module side effects.

### Phase 5 — purchasing
- [ ] Purchasing bounded context (domain → infra → api).
- [ ] Integration tests for payable lifecycle.

### Phase 6 — finance
- [ ] Finance bounded context (domain → infra → api).
- [ ] Aging report query against PostgreSQL.
- [ ] Integration tests for cash flow.

### Phase 7 — query
- [ ] Query bounded context scaffolding with `ElasticsearchClient` bean.
- [ ] Index mappings defined via the Elastic Java client (no Spring Data ES).
- [ ] Listeners projecting `SaleConfirmed` and other domain events into Elasticsearch indices.
- [ ] Query services: sales by seller, sales by period, revenue summary, stock turnover.
- [ ] GET-only REST endpoints under `/reports/**`.
- [ ] Integration tests using Testcontainers Elasticsearch.

### Phase 8 — fiscal
- [ ] Fiscal bounded context scaffolding.
- [ ] Java_NFe library integration (`github.com/Samuel-Oliveira/Java_NFe`).
- [ ] Digital certificate (A1) configuration per environment (homologação / produção).
- [ ] NFC-e emission: user invokes endpoint with sale reference → generates XML → signs → sends to SEFAZ → persists result.
- [ ] NF-e emission: user invokes endpoint with supplier return reference → generates XML → signs → sends to SEFAZ → persists result.
- [ ] Fiscal document status tracking (authorized, rejected, cancelled).
- [ ] Reissue flow on SEFAZ rejection.
- [ ] Domain events published after successful emission (`NfceEmitted`, `NfeEmitted`) for other modules to react.
- [ ] SEFAZ stub for homologação profile in integration tests.

### Phase 9 — frontend integration
- [ ] API contract review and cleanup.
- [ ] CORS configuration.
- [ ] Frontend repo scaffolding (to be defined).

### Phase 10 — legacy data migration
- [ ] ETL pipeline: MySQL (legacy Vaadin) → PostgreSQL (new schema).
- [ ] Data transformation and mapping for all bounded contexts.
- [ ] Validation: row counts, financial totals, referential integrity checks.
- [ ] Elasticsearch backfill from migrated PostgreSQL data.
- [ ] Cutover runbook and rollback plan.
- [ ] Detailed mapping and runbook documented in `migration-spec.md`.

---

## 7. Conventions and code standards

- Use cases are named in the imperative: `PlaceSaleUseCase`, `PayInstallmentUseCase`.
- One use case class per operation; no god services.
- Repository interfaces live in `domain`; implementations in `infrastructure`.
- DTOs never enter the domain layer.
- Enums are stored as `VARCHAR` in the database, not ordinal integers.
- Every entity has `createdAt` and `updatedAt` auto-managed by JPA auditing.
- No `@Transactional` on domain entities; only on use case or service methods.
- Unit tests cover domain logic; integration tests cover persistence + API.
- `ModularStructureTest` runs on every build via `modules.verify()` — a failing structure test blocks the build.
- Spring Security configuration lives exclusively in `config.infrastructure` — no other module defines security rules.
- **Entity base classes:** aggregate roots extend `BaseAggregate<T>` (audit fields + `registerEvent()`). Plain entities that are not aggregate roots extend `BaseEntity`. Never use `AbstractAggregateRoot` directly.
- **Entity construction:** entities have a `protected` no-arg constructor for JPA and a `static` factory method (`Entity.create(...)` or `Entity.of(...)`) as the only public instantiation point. `new Entity()` is never called from outside the domain.
- **No public setters:** state changes happen exclusively through named domain methods. Lombok `@Getter` is used on entity classes; `@Setter`, `@Data`, `@Builder` are prohibited. PUT use cases rely on JPA dirty checking — no explicit `save()` after loading an existing entity.
- **Domain invariants:** `IllegalArgumentException` for invalid input; `IllegalStateException` for invalid operation sequences. `GlobalExceptionHandler` maps them to `400` and `422` respectively. Domain classes never import Spring or HTTP types.
- Domain events are named in the past tense (`SaleConfirmed`, not `SaleConfirmEvent`).
- Listeners are never `@Transactional` themselves — they delegate to a use case that owns the transaction.
- No module imports from another module's sub-packages. Only `{module}.domain.event.*` records may be referenced across module boundaries.
- The `query` module never writes to PostgreSQL and never publishes domain events.
- All Elasticsearch operations in `query` use the official Elastic Java client (`co.elastic.clients`) directly. Spring Data Elasticsearch is not used anywhere in the project.
- Query endpoints are explicitly documented as eventually consistent in their OpenAPI descriptions.
- The `fiscal` module is user-initiated — emission is triggered explicitly via REST endpoints, never automatically by domain events. After successful emission it publishes its own events (`NfceEmitted`, `NfeEmitted`) for other modules to react.
- All SEFAZ communication is encapsulated in `fiscal.infrastructure.javaNfe`. No other layer touches the Java_NFe library directly.
- Digital certificate configuration is profile-scoped (homologação / produção) and managed exclusively within the `fiscal` module.