# Code Style — outfit (Java 21 / Spring Boot / Spring Modulith)

Full canonical patterns with code examples: `spec/architecture.md`.
Authoritative architecture rules: `CLAUDE.md`.

---

## Entity instantiation

- Aggregate roots: `static create(...)` factory method only — never `new` from outside the class
- Child entities: created exclusively via aggregate root domain methods
- All entities: `protected` no-arg constructor for JPA only
- Value objects (`@Embeddable`): `static of(...)` factory with validation; private constructor

## Lombok

Allowed on entities: `@Getter` **only**.

Banned: `@Setter`, `@Data`, `@Builder`, `@AllArgsConstructor`, `@RequiredArgsConstructor`.
- They bypass domain logic or expose public mutation.

DTOs are Java `record`s — no Lombok needed.

When a factory method has many parameters: hand-written static inner `Builder` class.
`build()` must delegate to the same validation logic as the private constructor.

## Mutation

No public setters. Mutation happens exclusively through named domain behavior methods:
`confirm()`, `deactivate()`, `updateProfile()`, etc.

PUT use cases: load entity → call domain method → JPA dirty-checking handles persistence.

**Exception:** use cases that need to publish domain events must call `repository.save(aggregate)`
explicitly so Spring Data reads `@DomainEvents`.

## Boolean fields

Plain names: `active`, `customer`, `supplier` — never `isActive`.
Lombok `@Getter` on `boolean active` generates `isActive()` automatically.

## DTO mapping

- Response DTOs: `static from(Entity)` factory method
- No mapping framework (MapStruct, ModelMapper)
- Use case passes request DTO fields individually to entity factory or domain method

## Base classes

| Type | Extends |
|---|---|
| Aggregate root | `BaseAggregate<T>` |
| Non-root entity | `BaseEntity` |
| Value object | `@Embeddable`, nothing |

Never extend `AbstractAggregateRoot` directly.

## Naming

| Element | Convention | Example |
|---|---|---|
| Classes | PascalCase | `PlaceSaleUseCase`, `SaleConfirmed` |
| Methods | camelCase, verb-first | `create()`, `findById()`, `markCompleted()` |
| Use case classes | imperative verb | `PlaceSaleUseCase`, `UpdateBrandUseCase` |
| Domain event records | past-tense | `SaleConfirmed`, `StockAdjusted` |
| Listeners | `{Action}Listener` | `IndexSaleListener` |
| Enum values | UPPER_SNAKE | `PENDING`, `DIRECT_SALE` |
| DB columns | snake_case | `gross_amount`, `created_at` |
| Test methods | `shouldXxxWhenYyy` | `shouldReturn404WhenSaleNotFound` |

## Method length

Target: ~15 lines max. Extract private methods with names that describe *what* each step does.
`execute()` in use cases reads as a high-level sequence of named steps, not inline code.

## Comments

WHY only — not WHAT. No multi-line comment blocks. No docstrings on internal classes.
Never reference the current task, fix, or caller — that belongs in the PR description.

## Logging

`@Slf4j` on use cases only — never on entities.

- Write ops: `INFO` after success
- Read-only use cases (Get/List): no `INFO` logging
- Failed auth: `WARN`
- Never log passwords, tokens, or PII
