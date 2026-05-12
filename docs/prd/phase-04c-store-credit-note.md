# PRD — Phase 4c: Store Credit Note

Depends on: docs/prd/phase-04b-sale-core.md

## Goal

Allow customers to exchange returned items for store credit. A store credit note is created with the full value of the returned items and can be applied as a complete discount on a future sale. The note is fully consumed in a single sale — partial usage is not supported.

---

## Scope

**In scope**

- `StoreCreditNote` aggregate and `StoreCreditItem` child entities
- Stock increment for returned items via `StoreCreditNoteCreated` event (inventory listens)
- `StockSource.RETURN` — new value added to the existing enum
- Application of a store credit note during sale creation (implemented in the `CreateSaleUseCase` already scaffolded in phase 4b)
- `StoreCreditNoteConsumed` event published when a note is applied to a sale
- REST endpoints for note creation and lookup

**Out of scope**

- Partial credit usage — a note is always consumed in full
- Refund to original payment method (cash refund, card reversal)
- Store credit query projections via Elasticsearch (phase 7)

---

## Domain Model

### StoreCreditNote

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `customerId` | Long | Party with customer role |
| `status` | `StoreCreditNoteStatus` | OPEN, CONSUMED |
| `totalValue` | BigDecimal | Sum of all item values; immutable after creation |
| `saleId` | Long | Nullable; set when consumed |
| `consumedAt` | Instant | Nullable; set when consumed |

### StoreCreditItem

Child entity of `StoreCreditNote`.

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `skuId` | Long | |
| `productId` | Long | Denormalized |
| `quantity` | int | |
| `unitValue` | BigDecimal | Value per unit at time of return |
| `totalValue` | BigDecimal | `unitValue × quantity` |

### StoreCreditNoteStatus

`OPEN`, `CONSUMED`

### StockSource (addition)

`RETURN` — added to the existing `StockSource` enum in the inventory module. Used when stock is incremented due to customer item returns recorded via a store credit note.

---

## Business Rules

- A store credit note must have at least one item.
- `totalValue` is immutable after creation.
- A note in CONSUMED status cannot be applied again.
- A note belongs to a specific customer and can only be applied to a sale for that same customer (validated in phase 4b's `CreateSaleUseCase`).
- The full `totalValue` is applied as `storeCreditDiscount` on the sale — no partial usage.

---

## Domain Events

### `StoreCreditNoteCreated`

Published when a store credit note is created. Consumed by the inventory module to increment stock for each returned item.

| Field | Type |
|---|---|
| `storeCreditNoteId` | Long |
| `customerId` | Long |
| `items` | List\<StoreCreditItemSnapshot\> |
| `totalValue` | BigDecimal |
| `createdAt` | Instant |

`StoreCreditItemSnapshot`: `skuId`, `productId`, `quantity`, `unitValue`

**Inventory listener:** `StoreCreditNoteCreatedListener` increments stock per item (source `RETURN`, sourceKey = `storeCreditNoteId`).

### `StoreCreditNoteConsumed`

Published when a note is applied to a sale. Carries the sale reference for auditability.

| Field | Type |
|---|---|
| `storeCreditNoteId` | Long |
| `customerId` | Long |
| `saleId` | Long |
| `totalValue` | BigDecimal |
| `consumedAt` | Instant |

---

## REST API

All endpoints require a valid JWT.

| Method | Path | Description |
|---|---|---|
| `POST` | `/store-credit-notes` | Create a store credit note (receive items) |
| `GET` | `/store-credit-notes/{id}` | Get note details |
| `GET` | `/store-credit-notes` | List notes (paginated) |

### Create — request body

```json
{
  "customerId": 12,
  "items": [
    { "skuId": 10, "productId": 3, "quantity": 1, "unitValue": 89.90 }
  ]
}
```

Returns `201 Created` with the note representation.

### List — query parameters

| Parameter | Type | Description |
|---|---|---|
| `customerId` | Long | Optional |
| `status` | String | Optional — OPEN or CONSUMED |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

Returns `PageResponse<StoreCreditNoteResponse>`.

### StoreCreditNoteResponse

```json
{
  "id": 301,
  "customerId": 12,
  "status": "OPEN",
  "totalValue": 89.90,
  "saleId": null,
  "consumedAt": null,
  "items": [
    {
      "id": 401,
      "skuId": 10,
      "productId": 3,
      "quantity": 1,
      "unitValue": 89.90,
      "totalValue": 89.90
    }
  ]
}
```

---

## Acceptance Criteria

- [ ] Creating a store credit note with 2 items — stock balance increases for each SKU; `StoreCreditNoteCreated` is published.
- [ ] `GET /store-credit-notes/{id}` returns the note with all items and `status = OPEN`.
- [ ] `GET /store-credit-notes?customerId=12&status=OPEN` returns only open notes for that customer.
- [ ] Creating a sale with a valid OPEN store credit note — `storeCreditDiscount` equals the full `totalValue`; note status changes to CONSUMED; `StoreCreditNoteConsumed` is published; `saleId` is set on the note.
- [ ] Attempting to apply a CONSUMED note to a sale — returns `422`.
- [ ] Attempting to apply a note belonging to a different customer — returns `422`.
- [ ] Stock increment for returned items uses source `RETURN`.
- [ ] `ModularStructureTest` passes.
- [ ] `./gradlew test` green.
