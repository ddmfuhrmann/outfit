# PRD — Phase 4d: Commission

Depends on: docs/prd/phase-04b-sale-core.md

## Goal

Calculate and record seller commission for every confirmed sale. Commission on immediate-payment installments is earned at sale time. Commission on deferred installments (INSTALLMENT, STORE_ACCOUNT) is recorded as pending and will be activated when the receivable is paid in the finance module (phase 6). Bonus tiers provide additional fixed amounts based on the sale's net value.

---

## Scope

**In scope**

- `SellerCommission` aggregate — one record per seller per sale
- `CommissionBonusTier` reference entity — configurable value ranges with fixed bonus amounts
- Listener on `SaleConfirmed` that creates commission records
- EARNED status for immediate-payment modalities
- PENDING status for INSTALLMENT and STORE_ACCOUNT modalities
- Bonus tier lookup and application at commission creation time
- REST endpoints for commission statement and bonus tier management

**Out of scope**

- Activating PENDING commissions (triggered by finance receivable payment in phase 6)
- Commission payout or payroll integration
- Commission query projections via Elasticsearch (phase 7)

---

## Domain Model

### SellerCommission

One record per seller per sale. When two sellers share a sale, two `SellerCommission` records are created.

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `saleId` | Long | |
| `salespersonId` | Long | |
| `sharePercent` | BigDecimal | Seller's share of the sale (from SaleSeller) |
| `commissionPercent` | BigDecimal | Rate in effect at sale time (from SaleSeller snapshot) |
| `baseAmount` | BigDecimal | `sale.netAmount × sharePercent / 100` |
| `earnedAmount` | BigDecimal | Commission on immediate-payment installments |
| `pendingAmount` | BigDecimal | Commission on deferred installments |
| `bonusAmount` | BigDecimal | Fixed bonus from matching CommissionBonusTier; 0 if no tier matches |
| `totalAmount` | BigDecimal | `earnedAmount + pendingAmount + bonusAmount` |
| `status` | `CommissionStatus` | EARNED (no deferred installments), PARTIAL (mixed), PENDING (all deferred) |

### CommissionStatus

`EARNED` — all commission is immediately earned (no deferred installments).  
`PARTIAL` — part earned immediately, part pending deferred payment.  
`PENDING` — all commission is deferred (entire sale on INSTALLMENT or STORE_ACCOUNT).

### CommissionBonusTier

Reference entity managed via REST. Defines a net sale value range that qualifies for a fixed bonus amount.

| Field | Type | Notes |
|---|---|---|
| `id` | Long (TSID) | |
| `lowerBound` | BigDecimal | Inclusive |
| `upperBound` | BigDecimal | Inclusive |
| `bonusAmount` | BigDecimal | Fixed bonus applied to the seller's commission |
| `active` | boolean | Inactive tiers are ignored in calculations |

Tiers must not overlap. The system matches the tier where `sale.netAmount` falls within `[lowerBound, upperBound]`. If no tier matches, `bonusAmount = 0`. The bonus applies once per sale regardless of seller count — each seller's `SellerCommission` includes the full `bonusAmount`.

---

## Commission Calculation

For each `SaleSeller` entry in the `SaleConfirmed` event:

1. `baseAmount = sale.netAmount × seller.sharePercent / 100`
2. Partition the sale's installments into immediate (CASH, CREDIT_CARD, DEBIT_CARD, CHECK, PIX, BANK_SLIP) and deferred (INSTALLMENT, STORE_ACCOUNT).
3. Compute each partition's proportion of `netAmount`.
4. `earnedAmount = baseAmount × seller.commissionPercent / 100 × immediateRatio`
5. `pendingAmount = baseAmount × seller.commissionPercent / 100 × deferredRatio`
6. Look up the active `CommissionBonusTier` where `sale.netAmount` is in `[lowerBound, upperBound]`. `bonusAmount` = tier's bonus or 0.
7. `totalAmount = earnedAmount + pendingAmount + bonusAmount`
8. Derive `status` from the partition: all immediate → EARNED; all deferred → PENDING; mixed → PARTIAL.

---

## Domain Events

No events published by this module in phase 4. Phase 6 will publish a `ReceivablePaid` event; this module will gain a listener at that time to activate PENDING commissions.

---

## REST API

All endpoints require a valid JWT.

| Method | Path | Description |
|---|---|---|
| `GET` | `/commissions` | Commission statement (paginated) |
| `GET` | `/commissions/{id}` | Get a single commission record |
| `POST` | `/commission-bonus-tiers` | Create a bonus tier |
| `GET` | `/commission-bonus-tiers` | List all bonus tiers |
| `PUT` | `/commission-bonus-tiers/{id}` | Update a bonus tier |
| `DELETE` | `/commission-bonus-tiers/{id}` | Deactivate a bonus tier |

### Commission statement — query parameters

| Parameter | Type | Description |
|---|---|---|
| `salespersonId` | Long | Optional |
| `status` | String | Optional — EARNED, PARTIAL, PENDING |
| `from` | LocalDate | Optional — sale date range start |
| `to` | LocalDate | Optional — sale date range end |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

Returns `PageResponse<SellerCommissionResponse>`.

### SellerCommissionResponse

```json
{
  "id": 501,
  "saleId": 201,
  "salespersonId": 5,
  "sharePercent": 100,
  "commissionPercent": 5.00,
  "baseAmount": 179.80,
  "earnedAmount": 8.99,
  "pendingAmount": 0,
  "bonusAmount": 0,
  "totalAmount": 8.99,
  "status": "EARNED"
}
```

### Bonus tier — request body

```json
{
  "lowerBound": 1000.00,
  "upperBound": 2999.99,
  "bonusAmount": 50.00
}
```

Returns `201 Created`. Returns `422` if the range overlaps with an existing active tier.

---

## Acceptance Criteria

- [ ] Confirming a sale paid entirely in CASH with one seller — one `SellerCommission` created with `status = EARNED`, `earnedAmount = netAmount × commissionPercent / 100`.
- [ ] Confirming a sale paid entirely in INSTALLMENT with one seller — `status = PENDING`, `earnedAmount = 0`, `pendingAmount = netAmount × commissionPercent / 100`.
- [ ] Confirming a mixed-payment sale (part CASH, part INSTALLMENT) — `status = PARTIAL`, `earnedAmount` and `pendingAmount` are proportional to each installment's share.
- [ ] Confirming a sale with two sellers — two `SellerCommission` records created, each with `sharePercent = 50` and `baseAmount = netAmount × 0.5`.
- [ ] Confirming a sale whose `netAmount` falls within an active bonus tier — `bonusAmount` equals the tier's fixed amount on each seller's commission record.
- [ ] Confirming a sale whose `netAmount` does not match any tier — `bonusAmount = 0`.
- [ ] Creating two overlapping bonus tiers — returns `422`.
- [ ] `GET /commissions?salespersonId=5&from=2026-05-01&to=2026-05-31` returns only commission records for that seller in that period.
- [ ] `ModularStructureTest` passes.
- [ ] `./gradlew test` green.
