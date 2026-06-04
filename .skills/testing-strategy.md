# Testing Strategy — outfit (JUnit 5 / Testcontainers / Spring Boot Test)

Full patterns with code examples: `spec/testing.md`.

---

## Infrastructure

All integration tests extend `AbstractIT`. It provides:
- Shared Testcontainers: PostgreSQL + Elasticsearch (started once per suite)
- `JdbcTemplate jdbcTemplate` for direct DB reads/writes
- `authHeader(String userId)` — generates a valid JWT for that user ID
- `WebTestClient webTestClient` — pre-configured with base URL

## Rules

- No `@MockBean` — use real containers
- No `@Transactional` on tests — it masks commit-level behavior
- No `@Sql` — use `JdbcTemplate` or REST calls to set up state
- No `sleep()` in query module tests — replication is synchronous (`SyncTaskExecutor`)

## Test type selection

| Scenario | Type |
|---|---|
| Pure domain logic, no I/O | Unit (no Spring context) |
| REST endpoint + database + ES | Integration (extends `AbstractIT`) |
| Cross-module event flow | Integration |

Default to integration for anything touching the database or Elasticsearch.

## Query module test pattern

Write via POST → assert ES document indexed → assert GET returns correct data:

```java
// 1. Write
webTestClient.post().uri("/sales").bodyValue(req)
    .headers(h -> h.addAll(authHeader(userId)))
    .exchange().expectStatus().isCreated();

// 2. Query (no sleep — synchronous replication)
webTestClient.get().uri("/query/sales?...")
    .headers(h -> h.addAll(authHeader(userId)))
    .exchange().expectStatus().isOk()
    .expectBody(PageResponse.class)...;
```

## Enrichment test pattern

When testing enrichment (customer name, product description, etc.):
1. Seed the referenced ES document first (e.g. index a party document directly via `ElasticsearchClient`)
2. Perform the write that triggers indexing
3. Assert the enriched fields appear in the query response

## Naming

- Test classes: `{FeatureName}IT` for integration, `{FeatureName}Test` for unit
- Test methods: `shouldXxxWhenYyy`
- One assertion per behavior
- Test failure paths as deliberately as success paths

## Running tests

```bash
# Full suite
./gradlew test

# Single class
./gradlew test --tests "github.io.ddmfuhrmann.outfit.SomeIT"

# Clean run
./gradlew cleanTest test
```

## What NOT to test

- Architecture rules are already enforced by `ModularStructureTest` — don't duplicate
- Don't write unit tests for logic that is already covered by integration tests
