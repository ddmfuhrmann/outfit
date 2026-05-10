# Architecture Spec

Reference for implementing new bounded contexts. Based on the `shared` module as the canonical example.

---

## Module Layout

Each bounded context is a plain package under `github.io.ddmfuhrmann.outfit`. No Gradle submodules.

```
{module}/
  domain/
    model/         ← entities, value objects, enums
    repository/    ← repository interfaces (ports)
    event/         ← domain event records (public cross-module API)
    exception/     ← module-specific exceptions
  application/
    usecase/       ← one class per operation
    dto/           ← request and response records
    listener/      ← @ApplicationModuleListener implementations
  infrastructure/
    persistence/   ← JPA repository implementations (if custom)
    security/      ← security beans (shared module only)
  api/
    rest/          ← controllers and global handlers
```

Spring Modulith enforces that no module imports from another module's sub-packages. Cross-module communication goes through `{module}.domain.event.*` records only.

---

## Domain Model

### Lombok rule

`@Getter` is the only Lombok annotation allowed on entities. It generates read access without opening mutation paths.

`@Setter`, `@Data`, `@Builder`, `@AllArgsConstructor`, and `@RequiredArgsConstructor` are prohibited on entities — they would bypass domain logic or expose public mutation. When an aggregate has many creation parameters, use a hand-written static inner `Builder` class: `build()` delegates to the same validation logic so all invariants stay in one place.

DTOs are Java records and need no Lombok at all.

### Base classes

Two base classes live in `shared.domain.model`. Choose based on whether the entity is an aggregate root:

| Class | Extends | Use when |
|---|---|---|
| `BaseEntity` | — | Plain entities and reference data that are **not** aggregate roots (e.g. `City`, `OrderItem`) |
| `BaseAggregate<T>` | `AbstractAggregateRoot<T>` | Entities that are aggregate roots and need to publish domain events (e.g. `User`, `Order`) |

Both provide `id`, `createdAt`, `updatedAt` via JPA auditing. Never extend `AbstractAggregateRoot` directly — always use `BaseAggregate<T>`.

`BaseEntity` and `BaseAggregate` do not use `@Getter` because they ship in `shared` and must remain free of Lombok as framework base classes. Subclasses annotate themselves with `@Getter`.

### Aggregate Roots vs. child entities

**Aggregate roots** are the consistency boundary of a bounded context. They:
- Extend `BaseAggregate<T>` to gain `registerEvent()`
- Are the only entry point for mutations — child entities are always changed through the root
- Have a `static create(...)` factory method that is the only public way to instantiate a new instance
- Publish domain events from within their own methods

**Child entities** (e.g., `OrderItem` inside `Order`) are owned exclusively by the aggregate root:
- Never instantiated directly from outside the root
- No factory method exposed publicly — the root creates them via a domain method (`order.addItem(...)`)
- Annotate with `@Getter`; no public setters

**Value objects** (e.g., `Cpf`, `Cnpj`) represent domain-specific types with their own validation rules:
- Annotated `@Embeddable` — JPA flattens their columns into the owning table, no join table
- `protected` no-arg constructor for JPA; private regular constructor; `static of(...)` factory that validates and returns the instance
- The `of()` factory strips formatting characters (`replaceAll("\\D", "")`) before validating, so callers can pass either formatted (`123.456.789-09`) or raw (`12345678909`) strings
- Do **not** extend `BaseEntity` or `BaseAggregate` — value objects have no identity of their own
- On the owning entity, annotate the field with `@Embedded`; the column name is declared inside the embeddable via `@Column`

```java
@Embeddable
public class Cpf {
    @Column(name = "cpf", length = 11)
    private String value;

    protected Cpf() {}
    private Cpf(String value) { this.value = value; }

    public static Cpf of(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("cpf is required");
        String digits = raw.replaceAll("\\D", "");
        validate(digits);  // length + check-digit logic
        return new Cpf(digits);
    }

    public String value() { return value; }
}

// In the owning entity:
@Embedded
private Cpf cpf;
```

**Reference data entities** (e.g., `City`) are effectively immutable from the application's perspective:
- Factory `of(...)` method for programmatic creation (seed scripts, imports)
- No domain behavior methods — the data never changes through the domain
- Annotate with `@Getter`; no public setters

### Entity structure

```java
@Entity
@Table(name = "customer")
@Getter
public class Customer extends BaseAggregate<Customer> {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false)
    private boolean active;

    protected Customer() {}  // JPA only — never call from business code

    public static Customer create(String name, String cpf) {
        var customer = new Customer();
        customer.name = name;
        customer.cpf = cpf;
        customer.active = true;
        customer.registerEvent(new CustomerRegistered(customer.cpf, customer.name));
        return customer;
    }

    public void updateProfile(String name) {
        this.name = name;
    }

    public void deactivate() {
        this.active = false;
        registerEvent(new CustomerDeactivated(this.cpf));
    }
}
```

Key rules:
- `protected Customer() {}` — required by JPA; never called from business code
- Fields assigned directly inside factory/domain methods (`customer.name = name`), not via setters
- `@Transactional` never goes on entities — only on use cases
- IDs: `BIGINT`, client-generated via TSID (`TsidCreator.getTsid().toLong()`) in the base class constructor — non-null from the moment the entity is instantiated, no `@GeneratedValue` needed
- Timestamps: `Instant` in Java, `TIMESTAMPTZ` in PostgreSQL
- Money: `BigDecimal` in Java, `NUMERIC(15,2)` in PostgreSQL — never `float` or `double`
- Enums: `@Enumerated(EnumType.STRING)` — never ordinal
- Associations: `@ManyToOne(fetch = FetchType.LAZY)` — always lazy
- **Boolean fields:** use plain names (`active`, `customer`, `supplier`) — never `is`-prefixed (`isActive`, `isCustomer`). Lombok's `@Getter` on a `boolean` field automatically generates the correct Java Bean accessor (`isActive()`, `isCustomer()`)

### Domain Invariants

Entities enforce their own rules regardless of the caller. DTO validation (`@NotBlank`, `@NotNull`) only protects the HTTP boundary.

| Situation | Exception | HTTP mapping |
|---|---|---|
| Invalid or missing input (null required field, wrong format) | `IllegalArgumentException` | `400 Bad Request` |
| Invalid operation sequence (e.g. deactivating an already-inactive entity) | `IllegalStateException` | `422 Unprocessable Entity` |

`GlobalExceptionHandler` in `shared.api.rest` maps both automatically. Domain classes never import Spring or HTTP types.

```java
public void deactivate() {
    if (!this.active) throw new IllegalStateException("Customer is already inactive");
    this.active = false;
    registerEvent(new CustomerDeactivated(this.cpf));
}

public static Customer create(String name, String cpf) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (cpf == null || !cpf.matches("\\d{11}")) throw new IllegalArgumentException("cpf must be 11 digits");
    // ...
}
```

### Domain Events

Records in `domain.event`, past-tense names, registered inside aggregate root methods:

```java
// domain/event/CustomerRegistered.java
public record CustomerRegistered(String cpf, String name) {}

// domain/event/CustomerDeactivated.java
public record CustomerDeactivated(String cpf) {}
```

Events are **never** published from use cases via `ApplicationEventPublisher`. The aggregate root registers them via `registerEvent()` inside the method that causes the state change. Spring Data publishes registered events automatically after `save()`.

> **Dirty checking caveat:** PUT and DELETE use cases rely on JPA dirty checking and do not call `save()` explicitly. This means `registerEvent()` calls inside domain methods (e.g. `deactivate()`) are stored on the entity but **not published** unless `save()` is called explicitly. When a listener needs to react to a soft-delete or update event, the use case must call `repository.save(entity)` before returning so Spring Data triggers event publication.

---

## Repositories

Interfaces in `domain.repository`, extending `JpaRepository`:

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCpf(String cpf);
    Page<Customer> findAllByActiveTrue(Pageable pageable);
}
```

- No `@Repository` annotation needed — Spring Data picks them up automatically
- Custom queries via Spring Data method naming or `@Query` for complex cases
- Implementations live in `infrastructure.persistence` only when manual `EntityManager` work is needed

---

## Use Cases

One class per operation, imperative name:

### POST — create via factory method

```java
@Service
public class RegisterCustomerUseCase {

    private final CustomerRepository customerRepository;

    public RegisterCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse execute(CreateCustomerRequest request) {
        var customer = Customer.create(request.name(), request.cpf());
        return CustomerResponse.from(customerRepository.save(customer));
        // registerEvent() events are published automatically after save
    }
}
```

### PUT — load, call domain method, no explicit save

```java
@Service
public class UpdateCustomerUseCase {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse execute(Long id, UpdateCustomerRequest request) {
        var customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        customer.updateProfile(request.name());
        // no save() call — JPA dirty checking detects the change within @Transactional
        return CustomerResponse.from(customer);
    }
}
```

### DELETE (soft) — domain method

```java
@Service
public class DeactivateCustomerUseCase {

    private final CustomerRepository customerRepository;

    @Transactional
    public void execute(Long id) {
        var customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        customer.deactivate();
    }
}
```

General rules:
- Constructor injection, no `@Autowired`
- Single `execute()` method
- `@Transactional` on `execute()` for write operations
- No HTTP or servlet types — use cases are framework-agnostic
- Never `new Entity()` from a use case — always go through the factory method

### Logging

Use cases use `@Slf4j` (Lombok). Never place loggers on entities.

| Operation | Level | What to log |
|---|---|---|
| POST / create | `INFO` | After `save()` — log the identifier and key discriminator (e.g. `login`, `role`, `id`) |
| PUT / update | `INFO` | After the domain method call — log `id` |
| DELETE (soft) / deactivate | `INFO` | After the domain method call — log `id` |
| Auth failure | `WARN` | Log the attempted `login` — not the password |
| Auth success | `INFO` | Log the `login` |
| GET / list (read-only) | — | Nothing at `INFO` — high frequency, no business value |

Never log sensitive data: passwords, password hashes, JWT tokens, or PII beyond the natural key (login, id).

```java
@Slf4j
@Service
public class RegisterCustomerUseCase {
    @Transactional
    public CustomerResponse execute(CreateCustomerRequest request) {
        var customer = Customer.create(request.name(), request.cpf());
        var response = CustomerResponse.from(customerRepository.save(customer));
        log.info("Customer created: id={}", response.id());
        return response;
    }
}
```

### Listeners

For cross-module reactions via domain events:

```java
@Service
public class UpdateStockOnSaleConfirmedListener {

    private final DecrementStockUseCase decrementStock;

    @ApplicationModuleListener
    public void on(SaleConfirmed event) {
        decrementStock.execute(event.saleId());
    }
}
```

- Annotated `@ApplicationModuleListener`, not `@EventListener`
- Never `@Transactional` — delegates to a use case that owns the transaction
- Named `{Action}Listener`

---

## Query Module (read side)

The `query` module is the read side of the CQRS split. It owns Elasticsearch exclusively and serves two types of frontend reads: cross-module list views and analytical queries. It never writes to PostgreSQL and never publishes domain events.

### When to use the query module

| Read type | Source | Why |
|---|---|---|
| List view spanning multiple modules (sales with customer + seller info) | `query` → Elasticsearch | Cross-module JOINs would violate module isolation |
| Filtered/searchable list with rich query parameters | `query` → Elasticsearch | Full-text search and faceting |
| Reports, dashboards, aggregations | `query` → Elasticsearch | Aggregation pipeline |
| Single entity by ID right after a write | owning module → PostgreSQL | Needs strong consistency (read-your-own-write) |
| Simple admin CRUD within a single module | owning module → PostgreSQL | No cross-module data, no need for search |

### Internal structure

```
query/
  domain/
    model/          ← projection records (flat, denormalized — not domain entities)
  application/
    listener/       ← @ApplicationModuleListener — consumes events, writes to ES
    queryservice/   ← reads from ES, returns projections to controllers
  infrastructure/
    elasticsearch/  ← ElasticsearchClient bean, index mapping definitions
  api/
    rest/           ← GET-only controllers — delegate to query services
```

### Projections

Projections are flat Java `record`s in `query.domain.model`. They are not domain entities — they carry no business logic and extend nothing. Each projection represents one Elasticsearch index document.

```java
// query/domain/model/SaleProjection.java
public record SaleProjection(
    String saleId,
    Instant confirmedAt,
    String customerName,
    String sellerLogin,
    List<String> productNames,
    BigDecimal total,
    String status
) {}
```

### Listeners

Listeners consume domain events from other modules and write projections to Elasticsearch:

```java
// query/application/listener/IndexSaleOnConfirmedListener.java
@Service
public class IndexSaleOnConfirmedListener {

    private final IndexSaleUseCase indexSale;

    @ApplicationModuleListener
    public void on(SaleConfirmed event) {
        indexSale.execute(event);
    }
}
```

The listener delegates to an `IndexSaleUseCase` (or equivalent) that owns the Elasticsearch write. This keeps the listener thin and lets the write logic be tested independently.

#### Synchronous indexing — default and intentional

`@ApplicationModuleListener` is backed by `@TransactionalEventListener(phase = AFTER_COMMIT)` and runs **synchronously in the same thread** after the PostgreSQL transaction commits. The actual execution order is:

```
POST /sales
  └─ transaction commits → sale persisted in PostgreSQL
       └─ AFTER_COMMIT: listener runs → document indexed in Elasticsearch
            └─ HTTP 201 returned to the client
```

This means Elasticsearch is consistent with PostgreSQL **before the HTTP response reaches the frontend**. The user can be redirected to a list view served by the `query` module immediately after a write and the new item will be present.

**This is a deliberate design decision for this phase.** The trade-off: Elasticsearch availability and indexing latency become part of the write path latency. This is acceptable at current scale.

#### Per-consumer async — mixing sync and async on the same event

Each listener independently decides whether it blocks the write thread. A single domain event can have multiple consumers with different strategies:

```
SaleConfirmed published
  ├─ IndexSaleListener                 → SYNC   (list view must be ready before HTTP 201)
  ├─ UpdateSalesDashboardListener      → ASYNC  (report projection — user is not redirected there)
  ├─ SendConfirmationEmailListener     → ASYNC  (side effect, does not affect UX flow)
  └─ RecalculateCommissionListener     → ASYNC + Event Publication Registry (critical, cannot be lost)
```

**Report and analytics listeners are always async.** The user is never redirected to a dashboard or report immediately after a write, so there is no UX contract requiring those projections to be ready before the HTTP response. Making them async frees the write thread from report aggregation work:

```java
@Service
public class UpdateSalesDashboardListener {

    private final UpdateSalesDashboardUseCase updateDashboard;

    @Async
    @ApplicationModuleListener
    public void on(SaleConfirmed event) {
        updateDashboard.execute(event);
    }
}
```

**List view listeners are synchronous.** Because the UX redirects the user to the list after a write, the index must be updated before the HTTP response returns:

```java
@Service
public class IndexSaleOnConfirmedListener {

    private final IndexSaleUseCase indexSale;

    @ApplicationModuleListener  // synchronous by default — no @Async
    public void on(SaleConfirmed event) {
        indexSale.execute(event);
    }
}
```

#### Async reliability — Event Publication Registry

`@Async` alone loses the event if the process crashes between the commit and the listener running. For critical async listeners (commission, email, fiscal side effects), use Spring Modulith's Event Publication Registry, which persists pending publications in PostgreSQL before delivering them and retries on failure (at-least-once delivery).

The project already includes `spring-modulith-starter-jpa` and the `V3__modulith_event_publication.sql` migration. Enable the registry in `application.yaml`:

```yaml
spring:
  modulith:
    events:
      jdbc:
        schema-initialization:
          enabled: true
```

#### Strategy selection guide

| Strategy | How | When |
|---|---|---|
| Synchronous (default) | `@ApplicationModuleListener` | Result must be available before HTTP response (list view ES indexing) |
| Async — fire and forget | `@Async` + `@ApplicationModuleListener` | Report/dashboard projections; non-critical side effects |
| Async — reliable | `@Async` + `@ApplicationModuleListener` + Event Publication Registry | Critical side effects that cannot be lost (email, commission, fiscal) |
| External broker | Kafka / RabbitMQ listener | Another system needs to consume the event, or horizontal scale is required |

#### Migrating list view listeners to async in the future

If write throughput demands it, list view indexing can also be made asynchronous. When that happens:

- The HTTP response returns before ES is updated
- The frontend must handle the gap: **optimistic UI** — add the new item to local state immediately using the POST response body, reconcile on next fetch
- OpenAPI descriptions on `query` endpoints already document this scenario — no contract change required, only frontend adaptation

### Query services

Query services are the read-side equivalent of use cases. They accept query parameters, execute an Elasticsearch query, and return a `PageResponse<Projection>`.

Naming: `List{Entity}QueryService` or `Search{Entity}QueryService`.

```java
// query/application/queryservice/ListSalesQueryService.java
@Service
public class ListSalesQueryService {

    private final ElasticsearchClient esClient;

    public ListSalesQueryService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PageResponse<SaleProjection> execute(SaleFilter filter, int page, int size) {
        var response = esClient.search(s -> s
            .index("sales")
            .query(buildQuery(filter))
            .from(page * size)
            .size(size)
            .sort(sort -> sort.field(f -> f.field("confirmedAt").order(SortOrder.Desc))),
            SaleProjection.class
        );
        // map hits to PageResponse
    }
}
```

Rules:
- No `@Transactional` — no PostgreSQL involved
- No `save()`, no repository — Elasticsearch only
- Return `PageResponse<Projection>`, not domain entities
- Never import from another module's sub-packages — only consume the projection data already indexed
- Document eventual consistency in the OpenAPI description of the endpoint

### Controllers in `query`

GET-only, delegate to query services:

```java
@RestController
@RequestMapping("/query/sales")
public class SaleQueryController {

    private final ListSalesQueryService listSales;

    @GetMapping
    @Operation(description = "Eventually consistent — reflects confirmed sales indexed from domain events.")
    ResponseEntity<PageResponse<SaleProjection>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            SaleFilter filter) {
        return ResponseEntity.ok(listSales.execute(filter, page, size));
    }
}
```

---

## DTOs

Java records with Bean Validation annotations. DTOs own the mapping in both directions — no mapping framework is used.

### Request DTOs

Fields are passed individually to the entity factory or domain method by the use case. The request record itself never enters the domain layer.

```java
public record CreateCustomerRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "\\d{11}") String cpf
) {}

public record UpdateCustomerRequest(
    @NotBlank String name
) {}
```

### Response DTOs

Carry a `from(Entity)` static factory that reads via getters:

```java
public record CustomerResponse(
    Long id,
    String name,
    String cpf,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
            c.getId(), c.getName(), c.getCpf(), c.isActive(),
            c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
```

- Records for immutability
- `from()` static factory reads from the entity via `@Getter`-generated methods
- Never expose sensitive fields (password hashes, raw tokens)
- If the compiler complains about a missing field in the record constructor, the mapping is visibly broken — this is intentional

### Pagination

All list responses use `PageResponse<T>`:

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages()
        );
    }
}
```

---

## REST Controllers

Controllers delegate exclusively to use cases:

```java
@RestController
@RequestMapping("/party/customers")
public class CustomerController {

    private final ListCustomersUseCase listCustomers;
    private final RegisterCustomerUseCase registerCustomer;
    private final UpdateCustomerUseCase updateCustomer;
    private final DeactivateCustomerUseCase deactivateCustomer;

    @GetMapping
    ResponseEntity<PageResponse<CustomerResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listCustomers.execute(PageRequest.of(page, size)));
    }

    @PostMapping
    ResponseEntity<CustomerResponse> create(@RequestBody @Valid CreateCustomerRequest request) {
        var created = registerCustomer.execute(request);
        return ResponseEntity.created(URI.create("/party/customers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    ResponseEntity<CustomerResponse> update(@PathVariable Long id,
                                            @RequestBody @Valid UpdateCustomerRequest request) {
        return ResponseEntity.ok(updateCustomer.execute(id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deactivateCustomer.execute(id);
        return ResponseEntity.noContent().build();
    }
}
```

### HTTP status conventions

| Situation | Status |
|---|---|
| Successful GET / PUT | `200 OK` |
| Successful POST | `201 Created` + `Location` header |
| Successful DELETE (soft) | `204 No Content` |
| Validation failure | `400 Bad Request` |
| Missing / invalid token | `401 Unauthorized` |
| Insufficient role | `403 Forbidden` |
| Resource not found | `404 Not Found` |
| Unique constraint violated | `409 Conflict` |
| Invalid operation sequence (domain invariant) | `422 Unprocessable Entity` |
| Unhandled exception | `500 Internal Server Error` |

Role enforcement via `@PreAuthorize("hasRole('ADMIN')")` at method or class level. `GlobalExceptionHandler` in `shared.api.rest` handles all exception types globally — do not add `@ExceptionHandler` to individual controllers.

### Error envelope

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with id 99",
  "path": "/party/customers/99",
  "timestamp": "2026-05-09T14:00:00Z"
}
```

---

## Security

All endpoints require a valid JWT except `/auth/**`. Tokens come from `POST /auth/login` and must be sent as `Authorization: Bearer <token>`.

JWT claims: `sub` (login), `name`, `role`, `iat`, `exp`. Signed with HS256. Secret loaded from `JWT_SECRET` env var (minimum 32 chars).

Configuration lives entirely in `shared.infrastructure.security` — other modules never touch security infrastructure.

---

## Flyway Migrations

Files at `src/main/resources/db/migration/`, named `V{N}__{description}.sql`:

```
V1__shared_schema.sql          ← city, company, app_user
V2__seed_admin_user.sql        ← default admin (BCrypt cost 12)
V3__modulith_event_publication.sql
V4__seed_company.sql
V5__party_schema.sql           ← next module adds V5
```

- One migration per logical change
- Never modify an existing migration — always add a new one
- Seed data gets its own migration file, separate from schema
- `spring.jpa.hibernate.ddl-auto: validate` — Flyway owns the schema, Hibernate only validates

### Column conventions in SQL

```sql
id          BIGINT PRIMARY KEY,
created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
amount      NUMERIC(15,2) NOT NULL,
status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
active      BOOLEAN      NOT NULL DEFAULT TRUE
```

---

## Configuration

`application.yaml` structure:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/outfit
  jpa:
    hibernate:
      ddl-auto: validate      # never create-drop or update in production
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

outfit:
  security:
    jwt:
      secret: ${JWT_SECRET}   # always from env; never hardcoded
      expiration-minutes: 60

springdoc:
  api-docs:
    path: /docs
```

Module-specific properties use the `outfit.{module}.*` namespace and are bound via `@ConfigurationProperties`.
