# PRD — Phase 3c: Stock Recount

Depends on: docs/prd/phase-03b-stock-control.md

## Goal

Allow warehouse operators to perform a physical stock count and reconcile discrepancies against the system balance. A recount follows a two-phase flow: items are registered while the recount is open, and closing it generates the necessary adjustment entries in the inventory ledger.

---

## Scope

**In scope**

- Create and manage a stock recount (open phase)
- Add SKUs and their physically counted quantities to an open recount
- Close a recount — generates ledger adjustment entries only where a discrepancy exists
- Fetch a recount with its items and their computed discrepancies

**Out of scope**

- Elasticsearch projections for recount data
- Partial recount (covering only a subset of SKUs) — the business may add any SKUs it wishes; there is no enforcement that all active SKUs must appear
- Automatic scheduling of recounts

---

## Domain model

### Stock recount

A recount is a bounded operation with a clear lifecycle. Once closed it is immutable.

| Field | Type | Notes |
|---|---|---|
| `date` | `LocalDate` | Date of the physical count |
| `notes` | `String` | Optional free-text notes |
| `status` | enum | `OPEN` or `CLOSED` |
| `closedAt` | `Instant` | Set when the recount is closed; `null` while open |

### Stock recount item

Each item represents one SKU included in the count.

| Field | Type | Notes |
|---|---|---|
| `productSkuId` | `Long` | The SKU being counted |
| `countedQty` | `int` | Physically counted quantity; must be ≥ 0 |

### Invariants

- Adding an item to a closed recount throws a domain error.
- The same SKU cannot appear twice in a single recount — a duplicate addition returns `400`.
- Closing an already-closed recount throws a domain error.
- `countedQty` must be ≥ 0.
- A recount with no items can be closed — it produces no adjustment entries.

### Adjustment logic on close

For each item in the recount:

1. Read the current balance from `StockBalance` for the SKU.
2. Compute `delta = countedQty − currentBalance`.
3. If `delta ≠ 0`: create a `StockEntry` with `source = RECOUNT_ADJUSTMENT`, `sourceKey = recountId`, `quantity = delta`.
4. If `delta = 0`: no entry is created for that SKU.

This is the same write path used by all other stock movements — `StockBalance` is updated and `StockEntryRecorded` is published for each adjustment entry.

---

## Domain events

### Consumed (indirectly)

Closing a recount triggers the standard stock entry creation path, which in turn publishes `StockEntryRecorded` for each discrepancy. No new events are introduced by this feature.

---

## REST API

All endpoints require a valid JWT.

| Method | Path | Description |
|---|---|---|
| `POST` | `/inventory/recount` | Open a new recount |
| `POST` | `/inventory/recount/{id}/items` | Add a SKU to an open recount |
| `POST` | `/inventory/recount/{id}/close` | Close the recount and apply adjustments |
| `GET` | `/inventory/recount/{id}` | Fetch the recount with items and discrepancies |

### Open recount — request body

```json
{
  "date": "2026-05-11",
  "notes": "Monthly count — warehouse A"
}
```

Returns `201 Created` with the recount id.

### Add item — request body

```json
{
  "skuId": 10,
  "countedQty": 18
}
```

Returns `204 No Content` on success.

### Close recount — no request body

Returns `204 No Content` on success. Closing is idempotent from the HTTP perspective — if already closed, returns `422`.

### Get recount — response

```json
{
  "id": 1,
  "date": "2026-05-11",
  "notes": "Monthly count — warehouse A",
  "status": "CLOSED",
  "closedAt": "2026-05-11T18:30:00Z",
  "items": [
    {
      "skuId": 10,
      "countedQty": 18,
      "systemBalance": 20,
      "delta": -2
    },
    {
      "skuId": 11,
      "countedQty": 5,
      "systemBalance": 5,
      "delta": 0
    }
  ]
}
```

`systemBalance` and `delta` are computed at response time from the current `StockBalance`. For a closed recount they reflect the balance at the moment of closing (since closing atomically updates the balances).

---

## Acceptance criteria

- [ ] Open a recount — returns `201` with id; status is `OPEN`.
- [ ] Add two items to an open recount — both appear in `GET /inventory/recount/{id}`.
- [ ] Add the same SKU twice to one recount — returns `400`.
- [ ] Add an item to a closed recount — returns `422`.
- [ ] Close a recount with one item where `countedQty ≠ systemBalance` — a `StockEntry(source=RECOUNT_ADJUSTMENT)` is created and `StockBalance` is updated.
- [ ] Close a recount where all items have `delta = 0` — no `StockEntry` records are created.
- [ ] Close an already-closed recount — returns `422`.
- [ ] `GET /inventory/recount/{id}` for a closed recount shows correct `delta` for each item.
- [ ] `StockEntryRecorded` is published for each adjustment entry generated on close.
- [ ] `ModularStructureTest` passes.
- [ ] `./gradlew test` green.
