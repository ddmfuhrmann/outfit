# PRD — Phase 2b: Party Module

## Goal

Deliver the `party` module covering customers, suppliers, and salespersons. All three roles are
unified under a single `Party` aggregate root with boolean flags, matching the legacy `ISSUER` model.
Each party can have multiple addresses and contacts managed through the aggregate.

---

## Scope

**In scope**

- `party` module scaffolding and Flyway migration
- `Party` aggregate root with role flags (customer, supplier, salesperson)
- `Address` and `Contact` as child entities managed through the aggregate
- Full CRUD for party + nested add/remove for addresses and contacts
- Domain events: `PartyCreated`, `PartyDeactivated`

**Out of scope**

- Elasticsearch projections (phase 7)
- Commission calculation (phase 4 — sales)
- Any link between party and sale/purchase documents (those modules own that relationship)

---

## Domain model

### Party

The central aggregate root. A party can simultaneously be a customer, supplier, and/or salesperson.

| Field | Type | Notes |
|---|---|---|
| `personType` | `PersonType` enum | `LEGAL_ENTITY`, `INDIVIDUAL` — stored as `VARCHAR` |
| `cnpj` | `String(14)` | Required when `personType = LEGAL_ENTITY` |
| `cpf` | `String(11)` | Required when `personType = INDIVIDUAL` |
| `companyName` | `String` | Required — used as display name for both person types |
| `tradeName` | `String` | Nullable |
| `isCustomer` | `boolean` | Default false |
| `isSupplier` | `boolean` | Default false |
| `isSalesperson` | `boolean` | Default false |
| `commissionPercent` | `BigDecimal` | Nullable; meaningful only when `isSalesperson = true` |
| `baseSalary` | `BigDecimal` | Nullable; meaningful only when `isSalesperson = true` |
| `active` | `boolean` | Default true |

At least one role flag must be `true` on creation. `deactivate()` throws `IllegalStateException` if already inactive.

### Address (child entity)

| Field | Type | Notes |
|---|---|---|
| `street` | `String` | Nullable |
| `neighborhood` | `String` | Nullable |
| `zipCode` | `String(8)` | Nullable |
| `number` | `String` | Nullable |
| `complement` | `String` | Nullable |
| `cityId` | `Long` | FK to `city.id` — stored as plain Long to respect module boundary |

### Contact (child entity)

| Field | Type | Notes |
|---|---|---|
| `classification` | `ContactType` enum | `PHONE`, `EMAIL`, `WHATSAPP`, `OTHER` — stored as `VARCHAR` |
| `description` | `String` | Required, non-blank |

### Domain events

| Event | Payload | Published when |
|---|---|---|
| `PartyCreated` | `partyId`, `companyName`, role flags | After successful `Party.create(...)` |
| `PartyDeactivated` | `partyId` | After `party.deactivate()` |

---

## REST API

All endpoints require a valid JWT.

| Method | Path | Description |
|---|---|---|
| `POST` | `/party` | Create party |
| `GET` | `/party` | Paginated list. Query params: `?role=customer\|supplier\|salesperson`, `?name=` |
| `GET` | `/party/{id}` | Full party with addresses and contacts |
| `PUT` | `/party/{id}` | Update profile fields |
| `DELETE` | `/party/{id}` | Deactivate (soft delete) |
| `POST` | `/party/{id}/addresses` | Add address |
| `DELETE` | `/party/{id}/addresses/{addressId}` | Remove address |
| `POST` | `/party/{id}/contacts` | Add contact |
| `DELETE` | `/party/{id}/contacts/{contactId}` | Remove contact |

---

## Acceptance criteria

- [ ] Create a LEGAL_ENTITY party — `400` if `cnpj` is missing.
- [ ] Create an INDIVIDUAL party — `400` if `cpf` is missing.
- [ ] Create a party with no role flag set — `400`.
- [ ] List parties filtered by role (`?role=customer`) returns only matching records.
- [ ] Add address and contact to an existing party; both appear in `GET /party/{id}`.
- [ ] Remove address and contact; neither appear in subsequent `GET`.
- [ ] Deactivate a party — subsequent `GET` shows `active: false`.
- [ ] Deactivate an already-inactive party returns `422`.
- [ ] `PartyCreated` and `PartyDeactivated` events are registered on the aggregate.
- [ ] `ModularStructureTest` passes — `party` does not import from other modules' sub-packages.
- [ ] All endpoints visible in OpenAPI at `GET /docs`.
- [ ] `./gradlew test` green.
