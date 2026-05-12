# PRD — Phase 4b: Sale Core

Depends on: docs/prd/phase-04a-consignment.md

## Goal

Implement the core sale transaction. A sale is created either directly by the seller or automatically when a consignment is closed. It is definitive at creation — no intermediate states. The sale records the full payment breakdown per modality, applies any store credit discount, and publishes a `SaleConfirmed` event that drives stock decrement (direct sales only), commission calculation, and future receivables creation in the finance module.

---

## Scope

**In scope**

- `Sale` aggregate with items, payment installments, seller entries, and store credit discount
- Sale creation via REST (direct sale)
- Sale creation via `ConsignmentClosed` event listener (consignment sale)
- `SaleConfirmed` event with full snapshot
- Inventory stock decrement for direct sales only
- `GET` endpoints for sale detail and list

**Out of scope**

- Commission calculation (phase 4d)
- Receivables and duplicata management (phase 6)
- Store credit note creation and consumption (phase 4c — the note entity must exist before it can be referenced here)
- Sale query projections via Elasticsearch (phase 7)

---

## Domain Model

### Sale

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `customerId` | Long | Nullable — walk-in sales allowed |
| `origin` | `SaleOrigin` | DIRECT, CONSIGNMENT |
| `consignmentId` | Long | Nullable; set only when origin = CONSIGNMENT |
| `saleDate` | LocalDate | |
| `grossAmount` | BigDecimal | Sum of all item totals before discounts |
| `storeCreditDiscount` | BigDecimal | Discount from a store credit note; default 0 |
| `storeCreditNoteId` | Long | Nullable; references the consumed note |
| `netAmount` | BigDecimal | `grossAmount − storeCreditDiscount` |
| `sellers` | List\<SaleSeller\> | One or two entries |

`Sale` requires optimistic locking (`@Version`).

### SaleItem

Child entity of `Sale`.

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `skuId` | Long | |
| `productId` | Long | Denormalized |
| `quantity` | int | |
| `unitPrice` | BigDecimal | |
| `itemDiscount` | BigDecimal | Per-item discount; default 0 |
| `totalPrice` | BigDecimal | `(unitPrice − itemDiscount) × quantity` |

### SaleInstallment

Child entity of `Sale`. Represents one payment entry (a sale may have multiple installments across different modalities).

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `paymentModality` | `PaymentModality` | |
| `amount` | BigDecimal | |
| `dueDate` | LocalDate | Nullable for immediate modalities |

The sum of all installment amounts must equal `netAmount`.

### SaleSeller

Embedded value object within `Sale`. Represents one seller's participation and commission share.

| Field | Type | Notes |
|---|---|---|
| `salespersonId` | Long | Reference to Party (salesperson role) |
| `sharePercent` | BigDecimal | Percentage of net amount attributed to this seller |
| `commissionPercent` | BigDecimal | Seller's commission rate at the time of sale |

When two sellers are provided, `sharePercent` defaults to 50 each. The sum of all `sharePercent` values must equal 100. `commissionPercent` is copied from the Party record at sale creation time and is immutable — it represents the rate that was in effect when the sale occurred.

### SaleOrigin

`DIRECT`, `CONSIGNMENT`

### PaymentModality

`CASH`, `CREDIT_CARD`, `DEBIT_CARD`, `CHECK`, `PIX`, `BANK_SLIP`, `INSTALLMENT`, `STORE_ACCOUNT`

`INSTALLMENT` and `STORE_ACCOUNT` are deferred modalities — Finance (phase 6) will create receivable records for installments with these modalities. All other modalities represent immediate receipt.

---

## Business Rules

- `grossAmount` must be greater than zero.
- `netAmount` must be greater than zero (`storeCreditDiscount` cannot exceed `grossAmount`).
- Total installment amounts must equal `netAmount`.
- A store credit note, when provided, must be in OPEN status and belong to the sale's customer.
- When two sellers are provided, `sharePercent` is set to 50 each by the system — the request only supplies the seller IDs.
- A consignment sale is created automatically from the `ConsignmentClosed` event; it cannot be created via REST with `origin = CONSIGNMENT`.

---

## Domain Events

### `SaleConfirmed`

Published immediately when a sale is created (direct or from consignment).

| Field | Type |
|---|---|
| `saleId` | Long |
| `customerId` | Long (nullable) |
| `origin` | SaleOrigin |
| `consignmentId` | Long (nullable) |
| `saleDate` | LocalDate |
| `grossAmount` | BigDecimal |
| `storeCreditDiscount` | BigDecimal |
| `netAmount` | BigDecimal |
| `items` | List\<SaleItemSnapshot\> |
| `installments` | List\<SaleInstallmentSnapshot\> |
| `sellers` | List\<SaleSellerSnapshot\> |

`SaleItemSnapshot`: `skuId`, `productId`, `quantity`, `unitPrice`, `itemDiscount`, `totalPrice`

`SaleInstallmentSnapshot`: `paymentModality`, `amount`, `dueDate`

`SaleSellerSnapshot`: `salespersonId`, `sharePercent`, `commissionPercent`

### Consumers

| Module | Condition | Action |
|---|---|---|
| `inventory` | `origin = DIRECT` | Decrement stock per item (source `SALE`) |
| `sales` (phase 4d) | Always | Calculate commission per seller |
| `finance` (phase 6) | Installments with INSTALLMENT or STORE_ACCOUNT | Create receivable records |

---

## Inventory Side Effect

The inventory module gains a `SaleConfirmedListener` that listens to `SaleConfirmed`. When `origin = DIRECT`, it records a stock decrement per sale item (source `SALE`, sourceKey = `saleId`). When `origin = CONSIGNMENT`, the listener takes no action — stock was already decremented during consignment issue.

---

## REST API

All endpoints require a valid JWT.

| Method | Path | Description |
|---|---|---|
| `POST` | `/sales` | Create a direct sale |
| `GET` | `/sales/{id}` | Get sale details |
| `GET` | `/sales` | List sales (paginated) |

### Create sale — request body

```json
{
  "customerId": 12,
  "saleDate": "2026-05-12",
  "storeCreditNoteId": null,
  "sellerIds": [5],
  "items": [
    { "skuId": 10, "productId": 3, "quantity": 2, "unitPrice": 89.90, "itemDiscount": 0 }
  ],
  "installments": [
    { "paymentModality": "CASH", "amount": 179.80, "dueDate": null },
    { "paymentModality": "CREDIT_CARD", "amount": 0, "dueDate": null }
  ]
}
```

Returns `201 Created` with the sale representation.

Returns `422` if:
- installment amounts do not sum to `netAmount`
- store credit note is not OPEN or does not belong to the customer
- `netAmount` is zero or negative

### List — query parameters

| Parameter | Type | Description |
|---|---|---|
| `customerId` | Long | Optional |
| `salespersonId` | Long | Optional — matches any seller in the sale |
| `origin` | String | Optional — DIRECT or CONSIGNMENT |
| `from` | LocalDate | Optional — sale date range start |
| `to` | LocalDate | Optional — sale date range end |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

Returns `PageResponse<SaleResponse>`.

### SaleResponse

```json
{
  "id": 201,
  "customerId": 12,
  "origin": "DIRECT",
  "consignmentId": null,
  "saleDate": "2026-05-12",
  "grossAmount": 179.80,
  "storeCreditDiscount": 0,
  "netAmount": 179.80,
  "sellers": [
    { "salespersonId": 5, "sharePercent": 100, "commissionPercent": 5.00 }
  ],
  "items": [
    {
      "skuId": 10,
      "productId": 3,
      "quantity": 2,
      "unitPrice": 89.90,
      "itemDiscount": 0,
      "totalPrice": 179.80
    }
  ],
  "installments": [
    { "paymentModality": "CASH", "amount": 179.80, "dueDate": null }
  ]
}
```

---

## Acceptance Criteria

- [ ] Creating a direct sale — `SaleConfirmed` is published with `origin = DIRECT`.
- [ ] Inventory decrements stock for each item of a direct sale.
- [ ] Closing a consignment — a sale is created automatically with `origin = CONSIGNMENT`; inventory does not decrement stock.
- [ ] Creating a sale with two sellers — each `SaleSeller` entry has `sharePercent = 50`.
- [ ] Creating a sale with installment amounts that do not sum to `netAmount` — returns `422`.
- [ ] Creating a sale with a store credit note belonging to a different customer — returns `422`.
- [ ] `GET /sales/{id}` returns all items, installments, and seller entries.
- [ ] `GET /sales?salespersonId=5` returns only sales where seller 5 participates.
- [ ] `GET /sales?origin=CONSIGNMENT` returns only consignment-originated sales.
- [ ] Consignment sale created from `ConsignmentClosed` is not exposed via `POST /sales`.
- [ ] `ModularStructureTest` passes.
- [ ] `./gradlew test` green — includes integration test for full consignment → sale flow with cross-module side effects.
