# Review Summary — Phase 4 Remaining

## Findings resolved before ship

| Severity | Finding | Resolution |
|---|---|---|
| BLOCKER | `StoreCreditNote` missing `@Version` — double-consume risk | Fixed: added `@Version` + `version` column in V13 |
| BLOCKER | `SellerCommission` missing `@Version` | Fixed: added `@Version` + `version` column in V14 |
| BLOCKER | `SaleInstallmentSnapshot.isDeferred()` used hardcoded strings — silent breakage if enum extended | Fixed: delegates to `PaymentModality.valueOf().isDeferred()` |
| WARNING | `sale_seller` table had no PRIMARY KEY — duplicate seller rows possible | Fixed: added `CONSTRAINT pk_sale_seller PRIMARY KEY (sale_id, salesperson_id)` in V12 |
| SUGGESTION | `Sale.applyStoreCreditDiscount()` was dead code | Fixed: removed method |

## Deferred findings

| Severity | Finding |
|---|---|
| WARNING | `CreateSaleUseCase` loads `StoreCreditNote` twice (once to validate, once to consume). Benign within same `@Transactional`. |
| WARNING | `CreateCommissionsFromSaleUseCase` passes live JPA `Sale` entity — tight temporal coupling within same transaction. |
| WARNING | `getSalespersonDetails.execute()` return value discarded in `buildSellers()` — duplicate lookup in commission use case. |
| WARNING | `CommissionBonusTier` extends `BaseAggregate` but has no `@Version` (non-financial config data). |

## Test result

163 tests, 163 passed — BUILD SUCCESSFUL (3 consecutive runs).
