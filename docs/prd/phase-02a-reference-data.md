# PRD — Phase 2a: Reference Data

## Goal

Deliver the reference data entities required by the product catalog: sizes, colors, brands, and categories.
Each entity is fully managed via CRUD endpoints. Mutations publish domain events so the `query` module can keep
Elasticsearch projections consistent via fan-out on write.

---

## Scope

**In scope**

- `catalog` module scaffolding (package structure only — product entities come in phase 2c)
- Flyway migration for all reference tables
- Full CRUD for: `Size`, `Color`, `Brand`, `Category`
- Domain events on rename: `SizeRenamed`, `ColorRenamed`, `BrandRenamed`, `CategoryRenamed`

**Out of scope**

- Product and ProductSku entities (phase 2c)
- Elasticsearch projections and listeners (phase 7)
- Seed data — reference data is managed by the admin via the API

---

## Domain model

All entities live in `catalog/domain/model/`. All extend `BaseAggregate<T>` and publish rename events.

| Entity | Key fields | Events |
|---|---|---|
| `Size` | `description` | `SizeRenamed` |
| `Color` | `description` | `ColorRenamed` |
| `Brand` | `description` | `BrandRenamed` |
| `Category` | `description`, `ncmCode` | `CategoryRenamed` |

Domain invariants:
- `description` is required and non-blank on all entities.
- Rename methods throw `IllegalArgumentException` on blank input.

---

## REST API

All endpoints require a valid JWT.

### Sizes

| Method | Path | Description |
|---|---|---|
| `POST` | `/catalog/sizes` | Create |
| `GET` | `/catalog/sizes` | Paginated list |
| `GET` | `/catalog/sizes/{id}` | Get by id |
| `PUT` | `/catalog/sizes/{id}` | Rename |

Same pattern for `/catalog/colors`, `/catalog/brands`, `/catalog/categories`.

All four entities expose `DELETE /{id}` returning `204 No Content`. Referential integrity is enforced by
PostgreSQL foreign keys — no application-level guard is needed. If a record is referenced by another table
the database will reject the delete and Spring will translate it to `409 Conflict` via `DataIntegrityViolationException`.

---

## Acceptance criteria

- [ ] Create, read, list, rename, and delete each reference entity via the API.
- [ ] `PUT` with a blank description returns `400`.
- [ ] `GET /{id}` for a non-existent id returns `404`.
- [ ] `DELETE /{id}` returns `204`; if the record is referenced by another table PostgreSQL rejects it and the API returns `409`.
- [ ] Rename operations register the correct domain event on the aggregate.
- [ ] `ModularStructureTest` passes — `catalog` does not import from other modules' sub-packages.
- [ ] All endpoints visible in OpenAPI at `GET /docs`.
- [ ] `./gradlew test` green.
