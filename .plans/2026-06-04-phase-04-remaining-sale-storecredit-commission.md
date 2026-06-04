---
date: 2026-06-04
title: phase-04-remaining-sale-storecredit-commission
---

# Phase 4 Remaining — Sale · Store Credit Note · Commission

## Understanding

Implement the 7 remaining Phase 4 tasks, completing the sales transaction lifecycle:

- **04b** — Sale Core: domain, API (including completing `CloseConsignmentUseCase`), and ES index
- **04c** — Store Credit Note: domain + API (no query module — served from PostgreSQL only)
- **04d** — Commission: domain + API (no query module — served from PostgreSQL only)

All 7 tasks ship in **one PR** on a single branch.

## Assumptions

1. All Phase 4a (Consignment) code is fully merged and functional — used as implementation patterns
2. `CreateSaleUseCase` (04b-2) is modified by two later phases: 04d-2 first (commission wiring), then 04c-2 (store credit discount + consume) — sequenced to minimize rework
3. `ElasticsearchIndexInitializer.createIndex()` uses a lambda-style `TypeMapping.Builder` signature, not a `Map<>` — implementation must follow the existing file style
4. Commissions and store credit notes have no ES index — served directly from PostgreSQL
5. Spring Modulith boundary: the two `SaleConfirmedListener` beans (one in `inventory`, one in `query`) are distinct classes in separate packages — no naming conflict

## Scope

### 04b-1 — Sale Domain
- **Flyway:** `V12__sale_schema.sql` — tables: `sale`, `sale_item`, `sale_installment`, `sale_seller`
- **Enums:** `SaleOrigin` (`DIRECT`, `CONSIGNMENT`), `PaymentModality` (8 values, `isDeferred()` returns true for `INSTALLMENT` and `STORE_ACCOUNT`)
- **Embeddable:** `SaleSeller` (`@Embeddable`, not a `BaseEntity`)
- **Entities:** `SaleItem extends BaseEntity` (static `create()`), `SaleInstallment extends BaseEntity` (static `create()`)
- **Aggregate:** `Sale extends BaseAggregate<Sale>` with `@Version long version`; `static Sale.create(SaleInput input)` factory with guards:
  - `grossAmount > 0`, `netAmount > 0`
  - sellers share percents sum to 100
  - installments sum to `netAmount` (±0.01 tolerance — use `BigDecimal.subtract().abs().compareTo()`)
- **Domain events:** `SaleItemSnapshot`, `SaleInstallmentSnapshot`, `SaleSellerSnapshot` records; `SaleConfirmed` carrying full sale snapshot
- **Repository:** `SaleRepository extends JpaRepository<Sale, Long>` with `findAll(Specification, Pageable)`

### 04b-2 — Sale API
- **`party/application/`:** create `package-info.java` with `@NamedInterface`; add `SalespersonDetails` record and `GetSalespersonDetailsService` service (reads `Party`, extracts display name and `commissionPercent`)
- **Replace stubs:**
  - `CreateSaleInstallmentRequest` — replace stub fields with `paymentModality: PaymentModality`, `dueDate: LocalDate`, `amount: BigDecimal`
  - `SaleResponse` — expand from stub (id, origin, consignmentId only) to full response with items, installments, sellers, amounts
- **New DTOs:** `CreateSaleItemRequest`, `CreateSaleRequest`, `SaleItemResponse`, `SaleInstallmentResponse`, `SaleSellerResponse`
- **Use cases:**
  - `CreateSaleUseCase` — computes amounts, fetches salesperson details via `GetSalespersonDetailsService`, assigns share percents, calls `Sale.create()`, saves, calls `CreateCommissionsFromSaleUseCase` placeholder (wired in 04d-2), returns `SaleResponse`
  - `CloseConsignmentUseCase` — complete the existing placeholder returning `null`; inject `CreateSaleUseCase`, build `CreateSaleRequest` from consignment sold items
- **Inventory listener:** `RecordSaleStockDecrementUseCase` + `SaleConfirmedListener` in `inventory/application/` — skips `CONSIGNMENT` origin, decrements stock for `DIRECT` using existing `StockMovementService.recordEntry()`
- **Controller:** `SaleController` at `/sales` — `POST /sales` only

### 04b-3 — Sale Query Module
- **`ElasticsearchIndexInitializer`:** add `INDEX_SALES = "sales"` constant; add index creation with `customerName` as `search_as_you_type` (follow existing lambda style, not Map<> style from task file)
- **Document records:** `SaleCustomerDocument`, `SaleSellerDocument`, `SaleItemDocument`, `SaleInstallmentDocument`, `SaleDocument` — in `query/application/dto/`
- **Indexing use case:** `IndexSaleUseCase` — enriches from `INDEX_PARTIES` and `INDEX_PRODUCTS` (ES-to-ES only, no PostgreSQL); pattern mirrors `IndexConsignmentUseCase`
- **Query listeners:** `SaleConfirmedListener` in `query/application/listener/` (independent from the inventory one) → delegates to `IndexSaleUseCase`
- **Read use cases:** `GetSaleFromIndexUseCase` (throws `QueryException` wrapping `EntityNotFoundException` if not found), `SearchSalesUseCase` with inner `SearchSalesQuery` record
  - `SearchSalesUseCase`: `multi_match` with `bool_prefix` on `customerName` fields; term filters for `customerId`, `salespersonId`, `status`, `issueDate` range; `bool` query (`must` for text, `filter` for terms)
- **Controller:** `SaleQueryController` at `/sales` — `GET /sales`, `GET /sales/{id}`; OpenAPI `@Operation` noting: "Served from Elasticsearch. Synchronous replication."

### 04c-1 — Store Credit Note Domain
- **`StockSource` enum:** add `RETURN` value (currently missing)
- **Flyway:** `V13__store_credit_note_schema.sql` — tables: `store_credit_note`, `store_credit_item`
- **Enum:** `StoreCreditNoteStatus` (`OPEN`, `CONSUMED`)
- **Entity:** `StoreCreditItem extends BaseEntity`
- **Aggregate:** `StoreCreditNote extends BaseAggregate<StoreCreditNote>` with:
  - `static create(...)` factory
  - `void consume(Long saleId)` — guard: `status == CONSUMED` → `IllegalStateException`; sets status to `CONSUMED`, links `saleId`, registers `StoreCreditNoteConsumed`
- **Domain events:** `StoreCreditItemSnapshot`, `StoreCreditNoteCreated`, `StoreCreditNoteConsumed`
- **Repository:** `StoreCreditNoteRepository extends JpaRepository<StoreCreditNote, Long>` with `findAll(Specification, Pageable)`

### 04c-2 — Store Credit Note API
*(implemented after 04d-2 to avoid rework on `CreateSaleUseCase`)*

- **DTOs:** `StoreCreditItemRequest`, `CreateStoreCreditNoteRequest`, `StoreCreditItemResponse`, `StoreCreditNoteResponse`
- **Use cases:**
  - `CreateStoreCreditNoteUseCase` — saves note, fires `StoreCreditNoteCreated`
  - `GetStoreCreditNoteUseCase` — read-only
  - `ListStoreCreditNotesUseCase` — paginated, filters: `customerId`, `status`
- **Update `CreateSaleUseCase`:** inject `StoreCreditNoteRepository`; when `storeCreditNoteId` non-null: load note, validate `OPEN` status and same `customerId`, set `storeCreditDiscount`, after `repository.save(sale)` call `note.consume(sale.getId())` and save note (fires `StoreCreditNoteConsumed`)
- **Inventory listener:** `RecordCreditReturnStockUseCase` + `StoreCreditNoteCreatedListener` in `inventory/application/` — increments stock using `StockSource.RETURN`
- **Controller:** `StoreCreditNoteController` at `/store-credit-notes` — `POST`, `GET /{id}`, `GET` (list)

### 04d-1 — Commission Domain
- **Flyway:** `V14__commission_schema.sql` — tables: `commission_bonus_tier`, `seller_commission`
- **Enum:** `CommissionStatus` (`EARNED`, `PARTIAL`, `PENDING`)
- **Aggregate:** `CommissionBonusTier extends BaseAggregate<CommissionBonusTier>` with `create()`, `update()`, `deactivate()`, `matches()` methods
- **Aggregate:** `SellerCommission extends BaseAggregate<SellerCommission>` — all commission math in `static create()` factory: base amount, immediate/deferred partition, commission base, `earnedAmount`, `pendingAmount`, `bonusAmount`, status derivation
- **Repositories:**
  - `CommissionBonusTierRepository` — `findByActiveTrue()`, `findActiveMatchingTier()` (@Query), `existsOverlappingActiveTier()` (@Query, uses `excludeId = -1L` for create scenario)
  - `SellerCommissionRepository` — `findAll(Specification, Pageable)`

### 04d-2 — Commission API
*(implemented before 04c-2)*

- **DTOs:** `SellerCommissionResponse`, `CommissionBonusTierResponse`, `CreateCommissionBonusTierRequest`, `UpdateCommissionBonusTierRequest`
- **Use cases:**
  - `CreateCommissionsFromSaleUseCase` — triggered from `CreateSaleUseCase`; looks up active bonus tier by `netAmount`; creates one `SellerCommission` per seller entry
  - Wire `CreateCommissionsFromSaleUseCase` into `CreateSaleUseCase` (replacing the placeholder from 04b-2)
  - `GetCommissionUseCase`, `ListCommissionsUseCase` (filters: `salespersonId`, `status`, date range)
  - `CreateCommissionBonusTierUseCase` (with overlap check via `existsOverlappingActiveTier()`), `UpdateCommissionBonusTierUseCase`, `DeactivateCommissionBonusTierUseCase`, `ListCommissionBonusTiersUseCase`
- **Controllers:**
  - `CommissionController` at `/commissions` — `GET /{id}`, `GET` (list)
  - `CommissionBonusTierController` at `/commission-bonus-tiers` — `POST`, `GET` (list), `PUT /{id}`, `DELETE /{id}`

## Out of scope

- Finance module (phase 6) — `SaleConfirmed` will be ready but no finance listener will be created
- Fiscal module (NFC-e/NF-e emission)
- Any changes to Phase 4a (Consignment) code

## Files likely to change

```
src/main/java/.../outfit/
  sales/domain/model/
    Sale.java                                   ← new
    SaleItem.java                               ← new
    SaleInstallment.java                        ← new
    SaleSeller.java                             ← new
    SaleOrigin.java                             ← new
    PaymentModality.java                        ← new
    StoreCreditNote.java                        ← new
    StoreCreditItem.java                        ← new
    StoreCreditNoteStatus.java                  ← new
    CommissionBonusTier.java                    ← new
    SellerCommission.java                       ← new
    CommissionStatus.java                       ← new
  sales/domain/event/
    SaleConfirmed.java + snapshots              ← new
    StoreCreditNoteCreated.java                 ← new
    StoreCreditNoteConsumed.java                ← new
    StoreCreditItemSnapshot.java                ← new
  sales/domain/repository/
    SaleRepository.java                         ← new
    StoreCreditNoteRepository.java              ← new
    CommissionBonusTierRepository.java          ← new
    SellerCommissionRepository.java             ← new
  sales/application/dto/
    CreateSaleInstallmentRequest.java           ← replace stub
    SaleResponse.java                           ← replace stub
    CreateSaleRequest.java                      ← new
    CreateSaleItemRequest.java                  ← new
    SaleItemResponse.java                       ← new
    SaleInstallmentResponse.java                ← new
    SaleSellerResponse.java                     ← new
    StoreCreditItemRequest.java                 ← new
    CreateStoreCreditNoteRequest.java           ← new
    StoreCreditItemResponse.java                ← new
    StoreCreditNoteResponse.java                ← new
    SellerCommissionResponse.java               ← new
    CommissionBonusTierResponse.java            ← new
    CreateCommissionBonusTierRequest.java       ← new
    UpdateCommissionBonusTierRequest.java       ← new
  sales/application/usecase/
    CreateSaleUseCase.java                      ← new (modified by 04d-2 and 04c-2 sequentially)
    CloseConsignmentUseCase.java                ← complete placeholder
    CreateStoreCreditNoteUseCase.java           ← new
    GetStoreCreditNoteUseCase.java              ← new
    ListStoreCreditNotesUseCase.java            ← new
    CreateCommissionsFromSaleUseCase.java       ← new
    GetCommissionUseCase.java                   ← new
    ListCommissionsUseCase.java                 ← new
    CreateCommissionBonusTierUseCase.java       ← new
    UpdateCommissionBonusTierUseCase.java       ← new
    DeactivateCommissionBonusTierUseCase.java   ← new
    ListCommissionBonusTiersUseCase.java        ← new
  sales/api/rest/
    SaleController.java                         ← new
    StoreCreditNoteController.java              ← new
    CommissionController.java                   ← new
    CommissionBonusTierController.java          ← new
  inventory/domain/model/StockSource.java       ← add RETURN
  inventory/application/
    RecordSaleStockDecrementUseCase.java        ← new
    SaleConfirmedListener.java                  ← new
    RecordCreditReturnStockUseCase.java         ← new
    StoreCreditNoteCreatedListener.java         ← new
  party/application/
    package-info.java                           ← new (@NamedInterface)
    SalespersonDetails.java                     ← new (record)
    GetSalespersonDetailsService.java           ← new
  query/infrastructure/config/
    ElasticsearchIndexInitializer.java          ← add INDEX_SALES + mapping
  query/application/dto/
    SaleDocument.java                           ← new
    SaleCustomerDocument.java                   ← new
    SaleSellerDocument.java                     ← new
    SaleItemDocument.java                       ← new
    SaleInstallmentDocument.java                ← new
  query/application/usecase/
    IndexSaleUseCase.java                       ← new
    GetSaleFromIndexUseCase.java                ← new
    SearchSalesUseCase.java                     ← new
  query/application/listener/
    SaleConfirmedListener.java                  ← new (distinct from inventory one)
  query/api/rest/
    SaleQueryController.java                    ← new

src/main/resources/db/migration/
  V12__sale_schema.sql                          ← new
  V13__store_credit_note_schema.sql             ← new
  V14__commission_schema.sql                    ← new

src/test/java/.../outfit/
  sales/SaleControllerIT.java                   ← new (4 tests)
  sales/SaleConfirmedListenerIT.java            ← new (2 tests)
  sales/ConsignmentControllerIT.java            ← enable @Disabled test
  sales/StoreCreditNoteControllerIT.java        ← new (6 tests)
  sales/CommissionControllerIT.java             ← new (8 tests)
  query/SaleQueryControllerIT.java              ← new (8 tests)
```

## Tests needed

| File | Count | Focus |
|---|---|---|
| `SaleControllerIT` | 4 | 201 created, stock decremented, two-seller share, installment mismatch 422 |
| `SaleConfirmedListenerIT` | 2 | DIRECT decrements stock, CONSIGNMENT does not double-decrement |
| `ConsignmentControllerIT` (enable 1) | 1 | `closeConsignmentReturns201WithSale` |
| `SaleQueryControllerIT` | 8 | enriched doc, quantities after return, status after close, search by name, status filter, 404 |
| `StoreCreditNoteControllerIT` | 6 | create, get, list, apply discount on sale, stock incremented, 422 already-consumed |
| `CommissionControllerIT` | 8 | create with bonus, list/filter, bonus tier CRUD, overlap conflict 422 |

## Risks

1. **`CreateSaleUseCase` touched by 3 phases** — 04b-2 creates it, 04d-2 wires commission, 04c-2 adds discount logic. Fully sequential on the same branch, but care needed not to introduce inconsistency in the domain method calls.
2. **Commission math precision** — `earnedAmount`, `pendingAmount`, `bonusAmount` derivation is complex; status depends on all three values. Unit-test the `SellerCommission.create()` factory directly if needed.
3. **`BigDecimal` comparison** — installment sum guard and commission amounts must never use `==` or `.equals()` for tolerance checks; always use `.subtract().abs().compareTo(tolerance) <= 0`.
4. **Two `SaleConfirmedListener` classes** — one in `inventory/application/listener/`, one in `query/application/listener/`. Different packages so no Spring bean collision, but must both be `@ApplicationModuleListener` and correctly scoped.
5. **`party/application/` @NamedInterface** — until the `package-info.java` is added, `CreateSaleUseCase` cannot inject `GetSalespersonDetailsService` without failing `ModularStructureTest`.

## Performance criteria

None beyond passing `./gradlew build` with all ITs green (Testcontainers PostgreSQL + Elasticsearch).

## API tests

| File | Covers |
|---|---|
| `api-tests/sale-flow.http` | **new** — direct sale (2 sellers, installments, stock decrement), query `GET /sales`, `GET /sales/{id}`, search by customer name, 422 installment mismatch |
| `api-tests/store-credit-flow.http` | **new** — create store credit note (return items), verify stock increment, apply note discount on a new sale, 422 re-use of consumed note |
| `api-tests/commission-flow.http` | **new** — create bonus tier, create sale, verify commission created with bonus, list commissions, deactivate tier, 422 overlapping tier |
| `api-tests/consignment-flow.http` | **update** — Step 20 (`close consignment`) must update the request body to the new `CreateSaleRequest` shape (replace `paymentMethod: String` stub with `paymentModality: PaymentModality`, add `dueDate`); capture `saleId` from response; add steps to verify `GET /sales/{{saleId}}` |

All new files must follow the standard format: numbered steps, `{{run}}` for uniqueness, ID capture via `client.global.set`, `### --- Error cases ---` section.

## Implementation order

```
1. 04b-1  Sale domain + Flyway V12
2. 04b-2  Sale API (party @NamedInterface, stubs, CreateSaleUseCase with placeholder, CloseConsignmentUseCase, inventory listener)
3. 04b-3  Sale Query (ES index, documents, use cases, listener, controller)
4. 04c-1  StoreCreditNote domain + Flyway V13 + StockSource.RETURN
5. 04d-1  Commission domain + Flyway V14
6. 04d-2  Commission API (wire into CreateSaleUseCase)
7. 04c-2  Store Credit API (update CreateSaleUseCase with discount + consume)
8. api-tests: sale-flow.http, store-credit-flow.http, commission-flow.http, update consignment-flow.http
```
