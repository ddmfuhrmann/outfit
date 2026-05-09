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

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

- `RANDOM_PORT` — avoids port conflicts in parallel runs
- `@ActiveProfiles("test")` — loads `application-test.yaml`
- Static `PostgreSQLContainer` starts once per JVM; Flyway runs on first connection and reuses the schema for all test classes in the same run
- `@DynamicPropertySource` wires the Testcontainers connection at runtime

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

    // --- helper ---

    String loginAsAdmin() {
        return rest.postForObject("/auth/login",
            new LoginRequest("admin", "admin"), LoginResponse.class).token();
    }

    HttpHeaders bearerHeaders(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    HttpHeaders bearerJsonHeaders(String token) {
        var headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

Always extract repeated HTTP boilerplate into private helpers. Keep test method bodies short and readable.

---

## Common Patterns

### GET — happy path

```java
@Test
void listReturnsPage() {
    String token = loginAsAdmin();
    var response = rest.exchange(
        "/party/customers?page=0&size=10",
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(token)),
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
    String token = loginAsAdmin();
    var request = new CreateCustomerRequest("Ana", "12345678901", "ana@example.com");
    var response = rest.exchange(
        "/party/customers",
        HttpMethod.POST,
        new HttpEntity<>(request, bearerJsonHeaders(token)),
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
    String token = loginAsAdmin();
    var created = rest.exchange(
        "/party/customers",
        HttpMethod.POST,
        new HttpEntity<>(new CreateCustomerRequest("Ana", "12345678901", null), bearerJsonHeaders(token)),
        CustomerResponse.class
    ).getBody();

    var response = rest.exchange(
        "/party/customers/" + created.id(),
        HttpMethod.PUT,
        new HttpEntity<>(new UpdateCustomerRequest("Ana Updated", null), bearerJsonHeaders(token)),
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
    String token = loginAsAdmin();
    var created = /* POST to create */ ...;

    var deleteResponse = rest.exchange(
        "/party/customers/" + created.id(),
        HttpMethod.DELETE,
        new HttpEntity<>(bearerHeaders(token)),
        Void.class
    );
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var getResponse = rest.exchange(
        "/party/customers/" + created.id(),
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(token)),
        CustomerResponse.class
    );
    assertThat(getResponse.getBody().active()).isFalse();
}
```

### 404 — not found

```java
@Test
void getUnknownIdReturns404() {
    String token = loginAsAdmin();
    var response = rest.exchange(
        "/party/customers/99999",
        HttpMethod.GET,
        new HttpEntity<>(bearerHeaders(token)),
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
    String adminToken = loginAsAdmin();
    rest.exchange("/shared/users", HttpMethod.POST,
        new HttpEntity<>(new CreateUserRequest("buyer", "pass1234", "Buyer", UserRole.USER), bearerJsonHeaders(adminToken)),
        UserResponse.class);

    String userToken = rest.postForObject("/auth/login",
        new LoginRequest("buyer", "pass1234"), LoginResponse.class).token();

    var response = rest.exchange(
        "/party/customers",
        HttpMethod.POST,
        new HttpEntity<>(new CreateCustomerRequest("X", "00000000001", null), bearerJsonHeaders(userToken)),
        String.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}
```

### 409 — duplicate / constraint violation

```java
@Test
void createDuplicateCpfReturns409() {
    String token = loginAsAdmin();
    var req = new CreateCustomerRequest("Ana", "12345678901", null);
    rest.exchange("/party/customers", HttpMethod.POST, new HttpEntity<>(req, bearerJsonHeaders(token)), CustomerResponse.class);

    var second = rest.exchange("/party/customers", HttpMethod.POST, new HttpEntity<>(req, bearerJsonHeaders(token)), String.class);
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
| Helper method | descriptive verb | `loginAsAdmin`, `bearerHeaders` |
| Module structure test | `ModularStructureTest` | (one file, root test package) |

---

## What Not To Do

- No `@MockBean` for use cases or repositories in integration tests — test the real stack
- No `@Transactional` on test methods — it changes rollback behavior and hides real DB state
- No `@Sql` scripts for test data setup — use `JdbcTemplate` or the API itself to keep setup readable
- No hardcoded ports or DB credentials — always from Testcontainers via `@DynamicPropertySource`
- No unit tests for thin use cases that just call a repository — integration tests cover these adequately
