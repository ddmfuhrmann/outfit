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
- Domain events: `PartyCreated`, `PartyUpdated`, `PartyDeactivated`, `PartyAddressAdded`, `PartyAddressRemoved`, `PartyContactAdded`, `PartyContactRemoved`
- Elasticsearch party index in the `query` module — all fields including nested addresses (with city name) and contacts, dynamic mapping
- `GET /party` (search) and `GET /party/{id}` served from Elasticsearch

**Out of scope**

- Commission calculation (phase 4 — sales)
- Any link between party and sale/purchase documents (those modules own that relationship)

---

## Domain model

### Party

The central aggregate root. A party can simultaneously be a customer, supplier, and/or salesperson.

| Field | Type | Notes |
|---|---|---|
| `personType` | `PersonType` enum | `LEGAL_ENTITY`, `INDIVIDUAL` — stored as `VARCHAR` |
| `cnpj` | `Cnpj` value object | Required when `personType = LEGAL_ENTITY`; validated via Módulo 11 check digits; stored as `VARCHAR(14)` digits-only |
| `cpf` | `Cpf` value object | Required when `personType = INDIVIDUAL`; validated via Módulo 11 check digits; stored as `VARCHAR(11)` digits-only |
| `legalName` | `String` | Required — official registered name (razão social / CPF name) |
| `name` | `String` | Nullable — display/preferred name (nome fantasia for companies, nickname for individuals) |
| `customer` | `boolean` | Default false |
| `supplier` | `boolean` | Default false |
| `salesperson` | `boolean` | Default false |
| `commissionPercent` | `BigDecimal` | Nullable; meaningful only when `salesperson = true` |
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
| `PartyCreated` | `partyId`, `legalName`, role flags | After successful `Party.create(...)` |
| `PartyUpdated` | `partyId` | After `party.updateProfile(...)` |
| `PartyDeactivated` | `partyId` | After `party.deactivate()` |
| `PartyAddressAdded` | `partyId` | After `party.addAddress(...)` |
| `PartyAddressRemoved` | `partyId`, `addressId` | After `party.removeAddress(...)` |
| `PartyContactAdded` | `partyId` | After `party.addContact(...)` |
| `PartyContactRemoved` | `partyId`, `contactId` | After `party.removeContact(...)` |

The `query` module listens to all seven events to keep the Elasticsearch index in sync, always reloading and re-indexing the full party document.

---

## REST API

All endpoints require a valid JWT.

### Write endpoints (`party` module)

| Method | Path | Description |
|---|---|---|
| `POST` | `/party` | Create party |
| `PUT` | `/party/{id}` | Update profile fields |
| `DELETE` | `/party/{id}` | Deactivate (soft delete) |
| `POST` | `/party/{id}/addresses` | Add address |
| `DELETE` | `/party/{id}/addresses/{addressId}` | Remove address |
| `POST` | `/party/{id}/contacts` | Add contact |
| `DELETE` | `/party/{id}/contacts/{contactId}` | Remove contact |

### Read endpoints (`query` module — served from Elasticsearch)

| Method | Path | Description |
|---|---|---|
| `GET` | `/party` | Search parties. Query params: `?q=` (searches `legalName`, `name`, `cnpj`, `cpf`), `?role=customer\|supplier\|salesperson`. Returns `PageResponse<PartyDocument>`. |
| `GET` | `/party/{id}` | Fetch party document by id from Elasticsearch. |

Read endpoints are eventually consistent: they reflect the state of the Elasticsearch index, which is updated asynchronously after each domain event. OpenAPI must document this.

---

## Elasticsearch index

Index name: `parties`

Dynamic mapping is enabled — no explicit mapping definition required. Addresses are embedded with city name enriched at index time so no join is needed at query time.

```json
{
  "id": 1,
  "personType": "LEGAL_ENTITY",
  "cnpj": "12345678000199",
  "cpf": null,
  "legalName": "Acme Ltda",
  "name": "Acme",
  "customer": true,
  "supplier": true,
  "salesperson": false,
  "commissionPercent": null,
  "active": true,
  "addresses": [
    {
      "id": 5,
      "street": "Rua das Flores",
      "neighborhood": "Centro",
      "zipCode": "01310100",
      "number": "42",
      "complement": null,
      "cityId": 1,
      "cityName": "São Paulo"
    }
  ],
  "contacts": [
    { "id": 3, "classification": "PHONE", "description": "11999999999" }
  ],
  "createdAt": "2026-05-09T12:00:00Z",
  "updatedAt": "2026-05-09T12:00:00Z"
}
```

The `query` module builds and updates this document by listening to all seven party events above. On each event it reloads the full party (including addresses and contacts) from PostgreSQL via `JdbcTemplate` and upserts the document.

---

## Acceptance criteria

- [ ] Create a LEGAL_ENTITY party — `400` if `cnpj` is missing.
- [ ] Create an INDIVIDUAL party — `400` if `cpf` is missing.
- [ ] Create a party with no role flag set — `400`.
- [ ] After creation, `GET /party/{id}` returns the party document from Elasticsearch.
- [ ] `GET /party?q=<legalName>` returns matching parties from Elasticsearch.
- [ ] `GET /party?role=customer` returns only parties where `customer: true`.
- [ ] Add address and contact to an existing party; both appear in subsequent `GET /party/{id}` (Elasticsearch).
- [ ] Remove address and contact; neither appear in subsequent `GET /party/{id}`.
- [ ] Deactivate a party — subsequent `GET /party/{id}` shows `active: false`.
- [ ] Deactivate an already-inactive party returns `422`.
- [ ] All seven domain events are registered on the aggregate at the correct operation.
- [ ] `ModularStructureTest` passes — `party` and `query` do not import from each other's sub-packages.
- [ ] All endpoints visible in OpenAPI at `GET /docs`; read endpoints note eventual consistency.
- [ ] `./gradlew test` green.
