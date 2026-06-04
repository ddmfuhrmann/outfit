# Error Handling — outfit (Java 21 / Spring Boot)

## Exception → HTTP status mapping

| Exception | HTTP | When to use |
|---|---|---|
| `IllegalArgumentException` | 400 Bad Request | Invalid input: missing or malformed field |
| `IllegalStateException` | 422 Unprocessable Entity | Invalid operation sequence (e.g. deactivating an already-inactive entity) |
| Constraint violation (DB) | 409 Conflict | Duplicate unique key |
| Entity not found | 404 Not Found | ID lookup returns empty |
| Unhandled | 500 Internal Server Error | Unexpected infra failure |

`GlobalExceptionHandler` owns all HTTP mapping — domain classes must never import Spring or HTTP types.

## Domain invariants

- Throw `IllegalArgumentException` inside entity/value object constructors and factory methods for invalid state
- Throw `IllegalStateException` inside domain methods for invalid operation sequences
- Domain methods enforce their own invariants immediately — no deferred validation

## Use cases

Use cases **never** catch and log exceptions — they propagate.
`GlobalExceptionHandler` is the sole logging point.

```java
// WRONG
try {
    sale.confirm();
} catch (IllegalStateException e) {
    log.warn("...", e);  // never here
    throw e;
}

// CORRECT — just let it propagate
sale.confirm();
```

## Logging severity

| Scenario | Level | Stack trace? |
|---|---|---|
| 5xx / infra failure | `log.error` | Yes |
| 409 / 400 (`IllegalArgumentException`) / 422 (`IllegalStateException`) | `log.warn` | No |
| Expected 4xx (404, 401, 403) | — | No logging |

Empty catch blocks are forbidden — minimum `log.warn(e.getMessage())`.

## Infrastructure exceptions

Elasticsearch failures:
- Index init failures → `IndexingException` (extends `ElasticsearchException`)
- Query failures → `QueryException` (extends `ElasticsearchException`)
- Never throw raw `RuntimeException` from infrastructure adapters
