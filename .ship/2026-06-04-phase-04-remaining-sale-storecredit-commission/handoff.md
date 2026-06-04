# Handoff — Phase 4 Remaining

## What was delivered

Phase 4 completes the sales transaction lifecycle:

- **Sale** (`POST /sales`) — direct sale or from consignment close. Validates sellers (∑ share = 100), installments (∑ ≈ netAmount ±0.01). Fires `SaleConfirmed`.
- **Sale Query** (`GET /sales`, `GET /sales/{id}`) — served from Elasticsearch (`INDEX_SALES`). Synchronous replication.
- **Store Credit Note** (`POST /store-credit-notes`, `GET /store-credit-notes/{id}`, `GET /store-credit-notes`) — creates a credit note from item returns. Adds stock back (`StockSource.RETURN`). Can be applied as discount on a future sale (`storeCreditNoteId` in `CreateSaleRequest`).
- **Commissions** (`GET /commissions`, `GET /commissions/{id}`, `/commission-bonus-tiers` CRUD) — one `SellerCommission` per seller per sale. Bonus tier applied by netAmount range.

## Flyway migrations

- V12: `sale`, `sale_item`, `sale_installment`, `sale_seller` (with PK constraint)
- V13: `store_credit_note`, `store_credit_item` (with optimistic locking)
- V14: `commission_bonus_tier`, `seller_commission` (with optimistic locking)

## Key architectural decisions in this PR

- `SaleConfirmed.origin` and `SaleInstallmentSnapshot.paymentModality` are `String` (not enum) because consumer modules (`inventory`, `query`) cannot reference `sales.domain.model` types. `isDeferred()` on the snapshot delegates to `PaymentModality.valueOf()` inside the `sales` module.
- Both `SaleConfirmedListener` beans use explicit `@Component` names (`"inventorySaleConfirmedListener"`, `"querySaleConfirmedListener"`) to avoid Spring bean name collision.
- `CreateSaleUseCase` handles store credit discount + consume within a single `@Transactional` call (double-load is benign here; tracked as deferred cleanup).

## Next step

Phase 5 — Purchasing module. PRD at `docs/prd/` — run `/bsdd-plan` with that PRD as input.

## Known risks

None. All BLOCKERs resolved before ship.
