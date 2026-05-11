# PRD — Phase 3a: Initial Stock Integration

## Goal

When a new product SKU is created in the catalog, the inventory module must automatically reserve a slot for it and record its opening stock quantity. This is the first and only automated entry point into the inventory ledger — all other movements (sales, purchases, adjustments, recounts) are user-initiated. The integration is event-driven: the catalog publishes `ProductSkuCreated` and the inventory module reacts without any direct coupling between the two.

---

## Scope

**In scope**

- Inventory module bootstrapping: `StockBalance` and `StockEntry` entities, `StockEntryRecorded` event
- Listener for `ProductSkuCreated` from the catalog module
- `StockBalance` creation for every new SKU regardless of opening quantity
- Initial `StockEntry` creation when `implantationQty > 0`
- `StockEntryRecorded` publication after the entry is persisted

**Out of scope**

- Manual stock adjustments (phase 3b)
- Stock recount (phase 3c)
- Elasticsearch projections (phase 3d)
- Any movement that does not originate from SKU creation

---

## Domain model

### Stock balance

Tracks the current balance for a single SKU. Created automatically when a SKU is first seen by the inventory module. There is exactly one `StockBalance` record per SKU for the lifetime of the system.

| Field | Type | Notes |
|---|---|---|
| `productSkuId` | `Long` | One record per SKU; natural primary key |
| `currentBalance` | `int` | Always equal to the `runningBalance` of the latest ledger entry |

Optimistic locking is required on this entity to prevent concurrent writes from producing inconsistent running balances. This matters most in later phases when sales and purchases update stock simultaneously.

### Stock entry

An immutable ledger record representing a single stock movement. Entries are never updated or deleted.

| Field | Type | Notes |
|---|---|---|
| `productSkuId` | `Long` | The SKU that moved |
| `productId` | `Long` | Denormalized from the SKU for grouping in the read store |
| `quantity` | `int` | Positive = inbound; negative = outbound |
| `runningBalance` | `int` | Cumulative balance for this SKU after this entry is applied |
| `source` | enum | Origin of the movement — `INITIAL_STOCK` for entries created here |
| `sourceKey` | `Long` | ID of the originating document; for `INITIAL_STOCK` this is the SKU id |
| `occurredAt` | `Instant` | Timestamp of the movement |

`runningBalance` on each entry equals `StockBalance.currentBalance` at the moment the entry was created plus `quantity`. Both are written atomically in the same transaction.

### Source enum

Defined here as part of the inventory module's domain; extended in later phases.

| Value | Phase | Produced by |
|---|---|---|
| `INITIAL_STOCK` | 3a | `ProductSkuCreated` listener |
| `MANUAL_ADJUSTMENT` | 3b | Manual adjustment endpoint |
| `RECOUNT_ADJUSTMENT` | 3c | Stock recount close |
| `SALE` | 4 | Sales module |
| `CONSIGNMENT` | 4 | Sales module |
| `PURCHASE` | 5 | Purchasing module |

### Invariants

- A `StockBalance` is always created when a new SKU is seen, even if `implantationQty = 0`.
- A `StockEntry` is only created when `implantationQty > 0`. Zero-quantity openings produce no ledger record.
- `StockEntryRecorded` is only published when a `StockEntry` is actually created.

---

## Domain events

### Consumed

| Event | From | Action |
|---|---|---|
| `ProductSkuCreated` | `catalog` | Creates `StockBalance(currentBalance = implantationQty)`; if `implantationQty > 0`, also creates `StockEntry(source = INITIAL_STOCK, sourceKey = skuId, quantity = implantationQty)` |

The `ProductSkuCreated` event already carries `skuId`, `productId`, and `implantationQty` — no additional query to the catalog is needed.

### Published

| Event | Payload | Published when |
|---|---|---|
| `StockEntryRecorded` | `entryId`, `skuId`, `productId`, `quantity`, `runningBalance`, `source`, `sourceKey`, `occurredAt` | After a `StockEntry` is persisted |

`StockEntryRecorded` is the **public API contract** between `inventory` and the `query` module (phase 3d). Its record shape must not change without coordinating that phase.

---

## REST API

This feature introduces no REST endpoints. All behaviour is internal, triggered exclusively by the `ProductSkuCreated` event.

---

## Acceptance criteria

- [ ] `ProductSkuCreated` with `implantationQty = 10` — `StockBalance(currentBalance = 10)` and `StockEntry(source = INITIAL_STOCK, quantity = 10, runningBalance = 10, sourceKey = skuId)` are created.
- [ ] `ProductSkuCreated` with `implantationQty = 0` — `StockBalance(currentBalance = 0)` is created; no `StockEntry` is recorded.
- [ ] `StockEntryRecorded` is published after a positive `implantationQty` entry.
- [ ] `StockEntryRecorded` is **not** published when `implantationQty = 0`.
- [ ] Creating two SKUs for the same product — each produces an independent `StockBalance`.
- [ ] `ModularStructureTest` passes — `inventory` listener only references `catalog.domain.event.ProductSkuCreated`, no sub-package imports.
- [ ] `./gradlew test` green.
