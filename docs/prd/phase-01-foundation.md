# PRD — Phase 1: Foundation

## Goal

Stand up the running skeleton that all future modules will build on: infrastructure, shared kernel, and stateless JWT authentication. By the end of this phase the application starts, the module structure is enforced, and a real login flow works end-to-end.

---

## Scope

**In scope**

- Docker Compose environment (PostgreSQL + Elasticsearch)
- Spring application configuration (`application.yaml`)
- `ModularStructureTest` wired and passing
- `shared` module: `City`, `Company`, `User` entities + Flyway V1 migration
- Shared kernel: base entity, JPA auditing, `PageResponse<T>`, global error handler
- Stateless JWT authentication: login endpoint, security filter chain

**Out of scope**

- Any other bounded context (party, catalog, inventory, …)
- User self-service (password reset, registration)
- Role management UI
- Refresh token rotation (Phase 1 issues non-refreshable tokens; refresh can be added later)

---

## Infrastructure

### Docker Compose

File: `docker-compose.yml` at project root.

| Service | Image | Port | Volume |
|---|---|---|---|
| `db` | `postgres:16` | `5432:5432` | `postgres_data:/var/lib/postgresql/data` |
| `es` | `elasticsearch:8.13.4` | `9200:9200` | `es_data:/usr/share/elasticsearch/data` |

Elasticsearch must start with `xpack.security.enabled=false` and `discovery.type=single-node` for local development.

Environment variables for the database:

```
POSTGRES_DB=outfit
POSTGRES_USER=outfit
POSTGRES_PASSWORD=outfit
```

### Application configuration

`src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: outfit
  datasource:
    url: jdbc:postgresql://localhost:5432/outfit
    username: outfit
    password: outfit
  jpa:
    hibernate:
      ddl-auto: validate        # never create/update — Flyway owns the schema
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

outfit:
  security:
    jwt:
      secret: ${JWT_SECRET}     # must be set via env var; min 32 chars
      expiration-minutes: 60
```

A `test` profile overrides the datasource to use Testcontainers — do not hard-code Testcontainers URLs in `application.yaml`.

---

## `shared` module

Package: `github.io.ddmfuhrmann.outfit.shared`

### Shared kernel (no persistence)

Lives directly in `shared` or `shared.domain`. These are utilities used by every module.

**`BaseEntity`**

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

Auditing must be enabled via `@EnableJpaAuditing` on the main application class or a dedicated `@Configuration`.

**`PageResponse<T>`**

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) { … }
}
```

**Global error handler**

`@RestControllerAdvice` mapping exceptions to a standard envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "City 99 not found",
  "path": "/shared/cities/99",
  "timestamp": "2026-05-09T14:00:00Z"
}
```

HTTP status mappings:

| Exception type | Status |
|---|---|
| `EntityNotFoundException` (JPA) / custom `ResourceNotFoundException` | 404 |
| `MethodArgumentNotValidException` | 400 |
| `DataIntegrityViolationException` | 409 |
| `AccessDeniedException` | 403 |
| Uncaught `Exception` | 500 |

---

### Domain model

**`City`** — read-only reference data; populated by seed migration, never created via API.

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | PK, auto-sequence |
| `ibgeCityCode` | `Integer` | not null, unique |
| `ibgeStateCode` | `Integer` | not null |
| `cityName` | `String` | not null |
| `stateName` | `String` | not null |
| `stateAbbr` | `String(2)` | not null |
| `createdAt` | `Instant` | audit |
| `updatedAt` | `Instant` | audit |

**`Company`** — single record representing the store. Created by seed migration; updated via API.

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | PK, always `1` |
| `cnpj` | `String(14)` | not null, unique |
| `companyName` | `String` | not null |
| `tradeName` | `String` | nullable |
| `street` | `String` | nullable |
| `phone` | `String` | nullable |
| `city` | `City` | FK, nullable |
| `createdAt` | `Instant` | audit |
| `updatedAt` | `Instant` | audit |

**`User`** — application users for authentication.

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | PK, auto-sequence |
| `login` | `String` | not null, unique |
| `passwordHash` | `String` | not null (BCrypt) |
| `name` | `String` | not null |
| `role` | `UserRole` (enum) | not null, default `USER` |
| `active` | `boolean` | not null, default `true` |
| `createdAt` | `Instant` | audit |
| `updatedAt` | `Instant` | audit |

`UserRole` values: `ADMIN`, `USER`. Stored as `VARCHAR`.

---

### Flyway migrations

**`V1__shared_schema.sql`** — creates tables for `City`, `Company`, `User`.

```sql
CREATE TABLE city (
    id           BIGSERIAL PRIMARY KEY,
    ibge_city_code  INTEGER NOT NULL UNIQUE,
    ibge_state_code INTEGER NOT NULL,
    city_name    VARCHAR(100) NOT NULL,
    state_name   VARCHAR(100) NOT NULL,
    state_abbr   CHAR(2) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE company (
    id           BIGSERIAL PRIMARY KEY,
    cnpj         CHAR(14) NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    trade_name   VARCHAR(200),
    street       VARCHAR(300),
    phone        VARCHAR(20),
    city_id      BIGINT REFERENCES city(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    login         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

> `user` is a reserved word in PostgreSQL — use `app_user`.

**`V2__seed_admin_user.sql`** — inserts the default admin. Password is the BCrypt hash of `admin` (changeme on first deploy).

```sql
INSERT INTO app_user (login, password_hash, name, role)
VALUES ('admin', '$2a$12$...', 'Administrator', 'ADMIN');
```

---

### REST API

All endpoints require a valid JWT except `POST /auth/login`. Roles marked `[ADMIN]` reject `USER` tokens with 403.

#### Cities

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/shared/cities` | any | Paginated list. Query params: `?page=0&size=20&search=` |
| `GET` | `/shared/cities/{id}` | any | Single city |

Cities are read-only from the API — they are populated by Flyway seed migrations.

#### Company

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/shared/company` | any | Company info |
| `PUT` | `/shared/company` | `ADMIN` | Update company info |

#### Users

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/shared/users` | `ADMIN` | Paginated list |
| `POST` | `/shared/users` | `ADMIN` | Create user |
| `GET` | `/shared/users/{id}` | `ADMIN` | Single user |
| `PUT` | `/shared/users/{id}` | `ADMIN` | Update (name, role, active) |
| `DELETE` | `/shared/users/{id}` | `ADMIN` | Deactivate (sets `active=false`; hard delete is not allowed) |

Response DTOs must never include `passwordHash`.

---

## Auth module

Package: `github.io.ddmfuhrmann.outfit.shared` (auth lives in shared; no separate auth module).

### Dependency

Add to `build.gradle.kts`:

```kotlin
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
```

### JWT design

| Claim | Value |
|---|---|
| `sub` | `user.login` |
| `name` | `user.name` |
| `role` | `user.role` (e.g. `ADMIN`) |
| `iat` | issued-at (epoch seconds) |
| `exp` | `iat + expirationMinutes * 60` |

Signed with HS256 using the secret from `outfit.security.jwt.secret`. Secret must be at least 32 characters.

### Endpoints

#### `POST /auth/login`

Request:
```json
{ "login": "admin", "password": "admin" }
```

Response `200 OK`:
```json
{
  "token": "eyJhbGci...",
  "expiresAt": "2026-05-09T15:00:00Z"
}
```

Errors:
- `401` — invalid credentials (same message regardless of which field was wrong)

#### Security filter chain

- All requests to `/auth/**` are permitted without a token.
- All other requests must carry a valid `Authorization: Bearer <token>` header.
- Invalid or expired tokens → `401`.
- Insufficient role → `403`.
- CSRF disabled (stateless API).
- Session creation policy: `STATELESS`.

---

## Spring Modulith

`ModularStructureTest` must be created and passing from the first commit:

```java
@ApplicationModuleTest
class ModularStructureTest {

    @Test
    void verifiesModularStructure(ApplicationModules modules) {
        modules.verify();
    }
}
```

The test lives at `src/test/java/github/io/ddmfuhrmann/outfit/ModularStructureTest.java`.

---

## Acceptance criteria

- [ ] `docker-compose up` starts PostgreSQL and Elasticsearch without errors.
- [ ] `./gradlew bootRun` starts the application against the running containers.
- [ ] Flyway applies V1 and V2 migrations cleanly on a fresh database.
- [ ] `POST /auth/login` with valid credentials returns a JWT.
- [ ] `POST /auth/login` with wrong credentials returns `401`.
- [ ] `GET /shared/cities` without a token returns `401`.
- [ ] `GET /shared/cities` with a valid `USER` token returns a paginated list.
- [ ] `POST /shared/users` with a `USER` token returns `403`.
- [ ] `POST /shared/users` with an `ADMIN` token creates a user and returns `201`.
- [ ] `GET /shared/company` returns the seeded company record.
- [ ] `ModularStructureTest.verifiesModularStructure` passes.
- [ ] `./gradlew test` passes with no skipped or failing tests.
- [ ] OpenAPI spec available at `GET /docs`.
