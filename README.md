# Outfit

Retail management system — migrated from a Vaadin legacy monolith to a modern Spring Boot backend. Also serves as a study project for clean architecture and DDD patterns.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x + Spring Modulith |
| Persistence | Spring Data JPA + Hibernate / PostgreSQL |
| Migrations | Flyway |
| Build | Gradle (Kotlin DSL) |
| Auth | Spring Security + JWT (stateless) |
| Read store | Elasticsearch (Elastic Java Client — no Spring Data ES) |
| Testing | JUnit 5 + Testcontainers + AssertJ |
| API Docs | SpringDoc OpenAPI 3 (`/docs`) |
| Containerization | Docker + Docker Compose |

## Architecture

Modular monolith with package-level bounded context isolation enforced by Spring Modulith. Boundaries are verified at test time — `ModularStructureTest` runs `modules.verify()` on every build.

A lightweight CQRS split separates the write side (PostgreSQL, all transactional operations) from the read side (Elasticsearch, analytical queries). Only the `query` module touches Elasticsearch; all other modules own their PostgreSQL schema exclusively and communicate via domain events.

See [`docs/project-spec.md`](docs/project-spec.md) for the full architecture spec and phased delivery plan.

## Running locally

```bash
./gradlew bootRun
./gradlew test
./gradlew build
```

Requires a running PostgreSQL instance. Docker Compose setup is planned as part of Phase 1.

## Bounded contexts

| Module | Responsibility |
|---|---|
| `shared` | Company, users, auth, cities |
| `party` | Customers, suppliers, sellers, addresses |
| `catalog` | Products, SKUs, taxes, reference data |
| `inventory` | Stock ledger, recounts, balance queries |
| `sales` | Orders, consignments, store credit, commissions |
| `purchasing` | Supplier purchases, payables, returns |
| `finance` | Cash register, receivables |
| `query` | Read-side projections (Elasticsearch) |
| `fiscal` | NFC-e / NF-e emission via SEFAZ |
