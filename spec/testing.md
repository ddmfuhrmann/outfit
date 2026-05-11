# Testing Spec

Patterns for integration tests in this project. Based on the `shared` module tests as the canonical example.

---

## Infrastructure

All integration tests extend `AbstractIT`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static final ElasticsearchContainer ELASTIC =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.13.4")
                    .withEnv("xpack.security.enabled", "false");

    static {
        POSTGRES.start();
        ELASTIC.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("elasticsearch.uris", () -> "http://" + ELASTIC.getHttpHostAddress());
    }

    private static String cachedToken;

    protected HttpHeaders authHeaders(TestRestTemplate rest) {
        if (cachedToken == null) {
            cachedToken = rest.postForObject("/auth/login",
                    new LoginRequest("admin", "admin"), LoginResponse.class).token();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cachedToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

- `RANDOM_PORT` — avoids port conflicts in parallel runs
- `@ActiveProfiles("test")` — loads `application-test.yaml`
- Static containers start once per JVM; Flyway runs on first connection and reuses the schema for all test classes in the same run
- `@DynamicPropertySource` wires the Testcontainers connection at runtime
- `authHeaders(rest)` caches the JWT in a static field — `/auth/login` is called once per Spring context, not once per test

### application-test.yaml

```yaml
outfit:
  security:
    jwt:
      secret: test-secret-key-must-be-at-least-32-chars-long
      expiration-minutes: 60
```

Only override properties that differ from `application.yaml` for tests. The datasource URL is set dynamically so it does not appear here.

---

## Integration Test Anatomy

```java
class CustomerControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void createReturns201() {
        var response = rest.exchange("/party/customers", HttpMethod.POST,
                new HttpEntity<>(new CreateCustomerRequest("Ana", "52998224725"), authHeaders(rest)),
                CustomerResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

- `authHeaders(rest)` is inherited from `AbstractIT` — no local copy needed
- Keep test method bodies short and readable

---

## Common Patterns

### GET — happy path

```java
@Test
void listReturnsPage() {
    var response = rest.exchange(
        "/party/customers?page=0&size=10",
        HttpMethod.GET,
        new HttpEntity<>(authHeaders(rest)),
        String.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("content");
}
```

### POST — 201 with Location

```java
@Test
void createReturns201() {
    var request = new CreateCustomerRequest("Ana", "52998224725", "ana@example.com");
    var response = rest.exchange(
        "/party/customers",
        HttpMethod.POST,
        new HttpEntity<>(request, authHeaders(rest)),
        CustomerResponse.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getBody().name()).isEqualTo("Ana");
}
```

### PUT — update and verify

```java
@Test
void updateReturns200() {
    var created = rest.exchange(
        "/party/customers",
        HttpMethod.POST,
        new HttpEntity<>(new CreateCustomerRequest("Ana", "52998224725", null), authHeaders(rest)),
        CustomerResponse.class
    ).getBody();

    var response = rest.exchange(
        "/party/customers/" + created.id(),
        HttpMethod.PUT,
        new HttpEntity<>(new UpdateCustomerRequest("Ana Updated", null), authHeaders(rest)),
        CustomerResponse.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().name()).isEqualTo("Ana Updated");
}
```

### DELETE (soft) — verify deactivated, not gone

```java
@Test
void deactivateReturns204AndKeepsRecord() {
    var created = /* POST to create */ ...;

    var deleteResponse = rest.exchange(
        "/party/customers/" + created.id(),
        HttpMethod.DELETE,
        new HttpEntity<>(authHeaders(rest)),
        Void.class
    );
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var getResponse = rest.exchange(
        "/party/customers/" + created.id(),
        HttpMethod.GET,
        new HttpEntity<>(authHeaders(rest)),
        CustomerResponse.class
    );
    assertThat(getResponse.getBody().active()).isFalse();
}
```

### 404 — not found

```java
@Test
void getUnknownIdReturns404() {
    var response = rest.exchange(
        "/party/customers/99999",
        HttpMethod.GET,
        new HttpEntity<>(authHeaders(rest)),
        String.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
}
```

### 401 — no token

```java
@Test
void protectedEndpointWithoutTokenReturns401() {
    var response = rest.getForEntity("/party/customers", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}
```

### 403 — wrong role

```java
@Test
void createWithUserRoleReturns403() {
    // Create a USER-role user first with ADMIN token
    rest.exchange("/shared/users", HttpMethod.POST,
        new HttpEntity<>(new CreateUserRequest("buyer", "pass1234", "Buyer", UserRole.USER), authHeaders(rest)),
        UserResponse.class);

    String userToken = rest.postForObject("/auth/login",
        new LoginRequest("buyer", "pass1234"), LoginResponse.class).token();
    var userHeaders = new HttpHeaders();
    userHeaders.setBearerAuth(userToken);
    userHeaders.setContentType(MediaType.APPLICATION_JSON);

    var response = rest.exchange(
        "/party/customers",
        HttpMethod.POST,
        new HttpEntity<>(new CreateCustomerRequest("X", "52998224725", null), userHeaders),
        String.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}
```

### 409 — duplicate / constraint violation

```java
@Test
void createDuplicateCpfReturns409() {
    var req = new CreateCustomerRequest("Ana", "52998224725", null);
    rest.exchange("/party/customers", HttpMethod.POST, new HttpEntity<>(req, authHeaders(rest)), CustomerResponse.class);

    var second = rest.exchange("/party/customers", HttpMethod.POST, new HttpEntity<>(req, authHeaders(rest)), String.class);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
}
```

### Direct DB operations

Use `JdbcTemplate` for test setup data or post-action assertions that bypass the API:

```java
@Autowired
JdbcTemplate jdbc;

@Test
void seedAndQuery() {
    Long id = jdbc.queryForObject(
        "INSERT INTO city (ibge_city_code, ibge_state_code, city_name, state_name, state_abbr, created_at, updated_at)" +
        " VALUES (4314902, 43, 'Porto Alegre', 'Rio Grande do Sul', 'RS', now(), now()) RETURNING id",
        Long.class);

    // now exercise the API endpoint that reads from city table
}
```

---

## Module Structure Test

Every project has exactly one `ModularStructureTest` at the root test package:

```java
class ModularStructureTest {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(OutfitApplication.class).verify();
    }
}
```

This test runs on every `./gradlew test` and fails the build if any module imports from another module's internal packages. It does not require a database.

---

## Naming Conventions

| What | Pattern | Example |
|---|---|---|
| Integration test class | `{Controller}IT` | `CustomerControllerIT` |
| Test method | `{subject}{condition}{expectedOutcome}` | `createWithUserRoleReturns403` |
| Helper method | descriptive verb | `createParty`, `createLegalEntity` |
| Module structure test | `ModularStructureTest` | (one file, root test package) |

---

## Valid Test Identifiers

Domain value objects (`Cpf`, `Cnpj`) validate format **and** check digits. Tests that create parties must use real valid values — invalid ones will throw `IllegalArgumentException` at the domain layer and fail unexpectedly.

Use these well-known valid numbers throughout the test suite:

| Type | Formatted | Digits only |
|---|---|---|
| CPF | `529.982.247-25` | `52998224725` |
| CNPJ | `11.222.333/0001-81` | `11222333000181` |

Both formats are accepted — the `of()` factory strips non-digit characters before validating.

---

## Manual API Tests (`.http` files)

The `api-tests/` directory contains IntelliJ HTTP Client files for manual exploration and smoke-testing against a running server. They complement the automated `*IT` tests — not replace them.

### Conventions

- One file per domain flow (e.g. `inventory-flow.http`), not one file per endpoint.
- Flow files run top-to-bottom: login first, create dependencies, then exercise the feature.
- Use `client.global.set(...)` response handlers to thread IDs between steps — no hardcoded IDs.
- Each step has a short comment explaining what it does and the expected outcome (status + state change).
- Error-path steps (4xx) come after the happy path; their comment must state the expected status code.
- Environment variables live in `http-client.env.json`. Never commit real credentials — the `local` environment uses the default dev `admin/admin` account.

### When to add or update a flow file

Every phase that introduces new write or query endpoints must either create a new `*-flow.http` or extend an existing one. Stale steps (referencing endpoints that don't exist yet) must be replaced or removed when the real endpoint ships.

---

## What Not To Do

- No `@MockBean` for use cases or repositories in integration tests — test the real stack
- No `@Transactional` on test methods — it changes rollback behavior and hides real DB state
- No `@Sql` scripts for test data setup — use `JdbcTemplate` or the API itself to keep setup readable
- No hardcoded ports or DB credentials — always from Testcontainers via `@DynamicPropertySource`
- No unit tests for thin use cases that just call a repository — integration tests cover these adequately
