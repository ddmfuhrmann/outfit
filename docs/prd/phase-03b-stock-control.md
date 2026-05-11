# PRD — Phase 3b: Stock Control

Depends on: docs/prd/phase-03a-initial-stock.md

## Goal

Extend the inventory ledger established in phase 3a with user-initiated write operations: manual balance adjustments and the movement history read. Manual adjustments allow operators to correct the stock of any SKU directly — without a source document — and are recorded as first-class ledger entries with their own source type.

---

## Scope

**In scope**

- Manual stock adjustment endpoint
- Paginated movement history endpoint per SKU

**Out of scope**

- Initial stock seeding from `ProductSkuCreated` (phase 3a)
- Stock decrements from sales and consignments (phase 4)
- Stock increments from purchases (phase 5)
- Stock recount flow (phase 3c)
- Elasticsearch read projections (phase 3d)
- Negative stock prevention — the system allows negative balances

---

## Domain model

### Stock entry

Each stock movement is an immutable ledger record. Entries are never updated or deleted.

| Field | Type | Notes |
|---|---|---|
| `productSkuId` | `Long` | References the SKU that moved |
| `productId` | `Long` | Denormalized from the SKU for query grouping |
| `quantity` | `int` | Positive = inbound; negative = outbound |
| `runningBalance` | `int` | Cumulative balance for this SKU after this entry |
| `source` | enum | Origin of the movement (see below) |
| `sourceKey` | `Long` | ID of the originating document; `null` for `MANUAL_ADJUSTMENT` |
| `occurredAt` | `Instant` | When the movement happened |

**Source values**

| Value | Produced by | `sourceKey` |
|---|---|---|
| `INITIAL_STOCK` | `ProductSkuCreated` listener | SKU id |
| `SALE` | Sales module (phase 4) | Sale id |
| `CONSIGNMENT` | Sales module (phase 4) | Consignment id |
| `PURCHASE` | Purchasing module (phase 5) | Purchase id |
| `RECOUNT_ADJUSTMENT` | Stock recount close (phase 3b) | Recount id |
| `MANUAL_ADJUSTMENT` | Manual adjustment endpoint | `null` |

### Stock balance

A single record per SKU that holds the current balance. It is always updated in the same transaction as the new ledger entry, preventing concurrent writes from producing inconsistent running balances.

| Field | Type | Notes |
|---|---|---|
| `productSkuId` | `Long` | One record per SKU (primary key) |
| `currentBalance` | `int` | Always equal to the `runningBalance` of the latest entry |

Optimistic locking is required on this entity — a conflicting concurrent update must be retried.

### Invariants

- The `runningBalance` of a new entry equals `currentBalance + quantity` at the moment of creation.
- `StockBalance` is created automatically with `currentBalance = 0` when the first `ProductSkuCreated` event arrives for a SKU.
- When `implantationQty = 0` on `ProductSkuCreated`, the `StockBalance` is created but no ledger entry is recorded.
- `quantity` must not be zero on a manual adjustment request.

---

## Domain events

### Published

| Event | Payload | Published when |
|---|---|---|
| `StockEntryRecorded` | `entryId`, `skuId`, `productId`, `quantity`, `runningBalance`, `source`, `sourceKey`, `occurredAt` | After every new stock entry is persisted |

`StockEntryRecorded` is the **public API contract** between the `inventory` module and the `query` module. Its shape must not change without coordinating phase 3c.

### Consumed

| Event | From | Action |
|---|---|---|
| `ProductSkuCreated` | `catalog` | Creates a `StockBalance` for the SKU; if `implantationQty > 0`, also creates the first `StockEntry` with `source = INITIAL_STOCK` |

---

## REST API

All endpoints require a valid JWT.

| Method | Path | Description |
|---|---|---|
| `POST` | `/inventory/adjustment` | Record a manual stock adjustment |
| `GET` | `/inventory/movements/{skuId}` | Paginated movement history for a SKU |

### Manual adjustment — request body

```json
{
  "skuId": 10,
  "desiredBalance": 25,
  "occurredAt": "2026-05-11T14:00:00Z"
}
```

The system computes `quantity = desiredBalance − currentBalance` and records the entry. Returns `204 No Content` on success.

If `desiredBalance` equals the current balance the request returns `422` — no entry would be produced.

### Movement history — response

```json
{
  "content": [
    {
      "id": 1,
      "quantity": 10,
      "runningBalance": 10,
      "source": "INITIAL_STOCK",
      "sourceKey": 10,
      "occurredAt": "2026-05-01T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Entries are ordered by `occurredAt` descending (most recent first).

---

## Acceptance criteria

- [ ] `ProductSkuCreated` with `implantationQty = 10` — a `StockBalance` and one `StockEntry(source=INITIAL_STOCK, quantity=10, runningBalance=10)` are created.
- [ ] `ProductSkuCreated` with `implantationQty = 0` — a `StockBalance(currentBalance=0)` is created and no `StockEntry` is recorded.
- [ ] `StockEntryRecorded` is published after every new `StockEntry`.
- [ ] `POST /inventory/adjustment` with `desiredBalance = 25` when current balance is 10 — creates `StockEntry(source=MANUAL_ADJUSTMENT, quantity=15, runningBalance=25)`.
- [ ] `POST /inventory/adjustment` with `desiredBalance` equal to the current balance — returns `422`.
- [ ] `GET /inventory/movements/{skuId}` returns entries ordered by `occurredAt` descending, paginated.
- [ ] Two concurrent manual adjustments for the same SKU — one succeeds and one is retried; the final `runningBalance` is consistent with both quantities applied.
- [ ] `ModularStructureTest` passes.
- [ ] `./gradlew test` green.
