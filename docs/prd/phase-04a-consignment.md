# PRD — Phase 4a: Consignment

## Goal

Implement the consignment flow — the primary pre-sale mechanism where items are issued to a customer, partially or fully returned, and the remaining sold items close the consignment and trigger a sale. Stock is decremented on issue and incremented on return within the consignment lifecycle.

---

## Scope

**In scope**

- `Consignment` aggregate with OPEN → CLOSED status
- `ConsignmentItem` child entities tracking issued, returned, and sold quantities
- Stock decrement on consignment issue (one entry per item)
- Stock increment on partial or full item returns
- Consignment close: publishes `ConsignmentClosed` event consumed by the sales module to create the sale
- REST endpoints for issue, return, and close operations

**Out of scope**

- Sale creation (triggered by `ConsignmentClosed` in phase 4b)
- Payment recording (phase 4b)
- Consignment query projections via Elasticsearch (phase 7)

---

## Domain Model

### Consignment

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `customerId` | Long | Reference to Party (customer role) |
| `salespersonIds` | List\<Long\> | One or two salesperson references (Party, salesperson role) |
| `status` | `ConsignmentStatus` | OPEN, CLOSED |
| `issueDate` | LocalDate | |
| `closedAt` | Instant | Null until closed |
| `notes` | String | Optional free-text observation |

### ConsignmentItem

Child entity of `Consignment`.

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `skuId` | Long | |
| `productId` | Long | Denormalized for stock entry |
| `unitPrice` | BigDecimal | Price at time of issue |
| `quantityIssued` | int | Set at issue; immutable after creation |
| `quantityReturned` | int | Incremented on each return operation |

`quantitySold` is derived: `quantityIssued − quantityReturned`. Carried in the `ConsignmentClosed` event for items where `quantitySold > 0`.

### ConsignmentStatus

`OPEN`, `CLOSED`

---

## Business Rules

- A consignment must have at least one item.
- `quantityReturned` cannot exceed `quantityIssued` for any item.
- Returns are not allowed on a CLOSED consignment.
- Closing a consignment that has no sold items (all quantities returned) is allowed — `ConsignmentClosed` is published with an empty item list.
- A CLOSED consignment cannot be re-opened or modified.
- Each item in the return request must reference a `skuId` that belongs to the consignment.

---

## Domain Events

### `ConsignmentClosed`

Published when the consignment is closed. Consumed by the sales module (phase 4b) to create the resulting sale.

| Field | Type |
|---|---|
| `consignmentId` | Long |
| `customerId` | Long |
| `salespersonIds` | List\<Long\> |
| `issueDate` | LocalDate |
| `closedAt` | Instant |
| `soldItems` | List\<ConsignmentItemSnapshot\> |

`ConsignmentItemSnapshot`: `skuId`, `productId`, `unitPrice`, `quantitySold`

Only items with `quantitySold > 0` are included in `soldItems`.

---

## Inventory Side Effects

Both side effects are handled by the inventory module listening to dedicated events or, for this phase, via direct calls from the use case through the inventory module's `@NamedInterface`. The preferred approach is event-driven:

- **On issue:** stock is decremented per item (source `CONSIGNMENT`)
- **On return:** stock is incremented per returned item (source `CONSIGNMENT`)

These are synchronous operations — they happen in the same transaction as the consignment write.

---

## REST API

All endpoints require a valid JWT.

| Method | Path | Description |
|---|---|---|
| `POST` | `/consignments` | Issue a new consignment |
| `GET` | `/consignments/{id}` | Get consignment details |
| `GET` | `/consignments` | List consignments (paginated) |
| `POST` | `/consignments/{id}/return-items` | Record item returns |
| `POST` | `/consignments/{id}/close` | Close the consignment |

### Issue — request body

```json
{
  "customerId": 12,
  "salespersonIds": [5],
  "issueDate": "2026-05-12",
  "notes": "optional",
  "items": [
    { "skuId": 10, "productId": 3, "quantity": 4, "unitPrice": 89.90 },
    { "skuId": 11, "productId": 3, "quantity": 2, "unitPrice": 89.90 }
  ]
}
```

Returns `201 Created` with the consignment representation.

### Return items — request body

```json
{
  "items": [
    { "skuId": 10, "quantityReturned": 2 }
  ]
}
```

Returns `200 OK` with the updated consignment. Returns `422` if a return quantity would exceed the issued quantity.

### List — query parameters

| Parameter | Type | Description |
|---|---|---|
| `customerId` | Long | Optional |
| `salespersonId` | Long | Optional |
| `status` | String | Optional — OPEN or CLOSED |
| `from` | LocalDate | Optional — issue date range start |
| `to` | LocalDate | Optional — issue date range end |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

Returns `PageResponse<ConsignmentResponse>`.

### ConsignmentResponse

```json
{
  "id": 1,
  "customerId": 12,
  "salespersonIds": [5],
  "status": "OPEN",
  "issueDate": "2026-05-12",
  "closedAt": null,
  "notes": null,
  "items": [
    {
      "id": 101,
      "skuId": 10,
      "productId": 3,
      "unitPrice": 89.90,
      "quantityIssued": 4,
      "quantityReturned": 2,
      "quantitySold": 2
    }
  ]
}
```

---

## Acceptance Criteria

- [ ] Issuing a consignment with 3 items — stock balance decreases by the issued quantity for each SKU.
- [ ] Returning 1 unit of a 4-unit item — `quantityReturned = 1`, `quantitySold = 3`; stock balance increases by 1 for that SKU.
- [ ] Attempting to return more units than issued — returns `422`.
- [ ] Returning items on a CLOSED consignment — returns `422`.
- [ ] Closing a consignment — status changes to CLOSED; `ConsignmentClosed` is published with only items where `quantitySold > 0`.
- [ ] Closing a consignment where all items were returned — `ConsignmentClosed` is published with an empty `soldItems` list.
- [ ] Closing an already CLOSED consignment — returns `422`.
- [ ] `GET /consignments/{id}` returns all items with correct derived `quantitySold`.
- [ ] `GET /consignments?status=OPEN` returns only open consignments.
- [ ] `ModularStructureTest` passes.
- [ ] `./gradlew test` green.
