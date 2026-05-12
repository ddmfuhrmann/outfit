# Tasks — Phase 4b: Sale API

Implements the sale write-side: DTOs, `CreateSaleUseCase`, the inventory listener for direct-sale stock decrement, and the `SaleController`. Also adds a `@NamedInterface` to the party module so the sales module can retrieve salesperson commission rates, and completes the `CloseConsignmentUseCase` placeholder from phase 4a-2.

GET endpoints (list and detail) are Elasticsearch-backed and live in the query module — see `docs/tasks/phase-04b-3-query-module.md`.

Depends on: docs/tasks/phase-04b-1-domain.md, docs/tasks/phase-04a-2-api.md

---

## 1. Party module: `@NamedInterface` for salesperson lookup

In `party/application/` (existing package).

- [ ] Create `party/application/package-info.java` annotated `@NamedInterface` — makes the application layer's Spring beans importable by other modules
- [ ] `SalespersonDetails.java` — `record SalespersonDetails(Long partyId, BigDecimal commissionPercent) {}` in `party/application/`
- [ ] `GetSalespersonDetailsService.java` — `@Service` in `party/application/`
  - `SalespersonDetails getDetails(Long partyId)` — loads the `Party` by ID (throw `EntityNotFoundException` → 404 if absent); throw `IllegalArgumentException` if the party is not a salesperson (`!party.isSalesperson()`); returns `new SalespersonDetails(party.getId(), party.getCommissionPercent())`

---

## 2. DTOs

In `sales/application/dto/`.

- [ ] `CreateSaleItemRequest.java` — `record CreateSaleItemRequest(@NotNull Long skuId, @NotNull Long productId, @Positive int quantity, @NotNull @Positive BigDecimal unitPrice, @NotNull @PositiveOrZero BigDecimal itemDiscount) {}`
- [ ] `CreateSaleInstallmentRequest.java` — `record CreateSaleInstallmentRequest(@NotNull PaymentModality paymentModality, @NotNull @Positive BigDecimal amount, LocalDate dueDate) {}`
  - Complete the stub created in phase 4a-2 if it was left as a placeholder
- [ ] `CreateSaleRequest.java` — `record CreateSaleRequest(Long customerId, @NotNull LocalDate saleDate, Long storeCreditNoteId, @NotEmpty List<Long> sellerIds, @NotEmpty List<CreateSaleItemRequest> items, @NotEmpty List<CreateSaleInstallmentRequest> installments) {}`
- [ ] `SaleItemResponse.java` — `record SaleItemResponse(Long id, Long skuId, Long productId, int quantity, BigDecimal unitPrice, BigDecimal itemDiscount, BigDecimal totalPrice)` with `static from(SaleItem)`
- [ ] `SaleInstallmentResponse.java` — `record SaleInstallmentResponse(Long id, String paymentModality, BigDecimal amount, LocalDate dueDate)` with `static from(SaleInstallment)`
- [ ] `SaleSellerResponse.java` — `record SaleSellerResponse(Long salespersonId, BigDecimal sharePercent, BigDecimal commissionPercent)`
- [ ] `SaleResponse.java` — `record SaleResponse(Long id, Long customerId, String origin, Long consignmentId, LocalDate saleDate, BigDecimal grossAmount, BigDecimal storeCreditDiscount, BigDecimal netAmount, List<SaleSellerResponse> sellers, List<SaleItemResponse> items, List<SaleInstallmentResponse> installments)` with `static from(Sale)`

---

## 3. Use cases

In `sales/application/usecase/`.

- [ ] `CreateSaleUseCase.java` — `@Service @Transactional`
  - Constructor-inject: `SaleRepository`, `GetSalespersonDetailsService`, `CreateCommissionsFromSaleUseCase` (added in phase 4d-2; use a lazy injection or leave as a placeholder until phase 4d-2)
  - `SaleResponse execute(CreateSaleRequest request, SaleOrigin origin, Long consignmentId)`
    1. Compute `grossAmount` = sum of `(unitPrice − itemDiscount) × quantity` per item
    2. Compute `netAmount = grossAmount` (store credit handled in phase 4c-2)
    3. Guard: installment amounts sum must equal `netAmount` (tolerance ±0.01) → `IllegalArgumentException`
    4. Fetch `SalespersonDetails` for each seller ID via `GetSalespersonDetailsService`
    5. Assign `sharePercent`: 100 if 1 seller, 50 each if 2 sellers
    6. Build `Sale.SaleInput` and call `Sale.create(input)`
    7. Call `saleRepository.save(sale)` explicitly (fires `SaleConfirmed` event)
    8. Call `createCommissionsUseCase.execute(sale)` (placeholder: wire in phase 4d-2)
    9. Log `INFO` with sale ID and net amount
    10. Return `SaleResponse.from(sale)`
  - Overload convenience: `SaleResponse execute(CreateSaleRequest request)` — calls `execute(request, SaleOrigin.DIRECT, null)`

- [ ] Complete `CloseConsignmentUseCase.java` (placeholder from phase 4a-2)
  - Inject `CreateSaleUseCase`
  - After `consignment.close()` and `repository.save(consignment)`:
    - Build `CreateSaleRequest` from sold items + request installments + sellerIds
    - Call `createSaleUseCase.execute(request, SaleOrigin.CONSIGNMENT, consignment.getId())`
  - Return the `SaleResponse`

---

## 4. Inventory listener: sale stock decrement

In `inventory/application/`.

- [ ] `RecordSaleStockDecrementUseCase.java` — `@Service @Transactional`
  - `void execute(SaleConfirmed event)`
  - Guard: if `event.origin().equals("CONSIGNMENT")` → return immediately (stock already decremented at consignment issue)
  - For each item: calls `StockMovementService.recordEntry(item.skuId(), item.productId(), -item.quantity(), StockSource.SALE, event.saleId(), event.saleDate().atStartOfDay(ZoneOffset.UTC).toInstant())`

- [ ] `SaleConfirmedListener.java` — `@Component` in `inventory/application/listener/`
  - `@ApplicationModuleListener void on(SaleConfirmed event)` → delegates to `RecordSaleStockDecrementUseCase`

---

## 5. Controller

In `sales/api/rest/`.

- [ ] `SaleController.java` — `@RestController @RequestMapping("/sales")`
  - `POST /sales` → `CreateSaleUseCase.execute(request)` → `201 Created` + `Location: /sales/{id}`
  - OpenAPI `@Operation` on each method
  - GETs live in `query/api/rest/SaleQueryController` (phase 04b-3)

---

## 6. Integration tests

In `src/test/java/.../outfit/sales/`.

- [ ] `SaleControllerIT.java` — extends `AbstractIT`
  - `createDirectSaleReturns201` — valid request, one seller, CASH installment → 201; response has `origin = DIRECT`
  - `createDirectSaleDecrementsStock` — after sale, assert stock movement entry exists with source SALE for each SKU
  - `createDirectSaleWithTwoSellersCreatesCorrectSharePercent` — two seller IDs → each `SaleSeller` has `sharePercent = 50`
  - `createSaleWithInstallmentSumMismatchReturns422` — installments don't sum to netAmount → 422

- [ ] `SaleConfirmedListenerIT.java` — extends `AbstractIT`
  - `directSaleDecrementsStock` — POST /sales → verify inventory has a SALE decrement entry per item
  - `consignmentSaleDoesNotDecrementStockAgain` — close a consignment → verify no duplicate SALE decrement (only the CONSIGNMENT decrement from issue remains)

- [ ] Complete `closeConsignmentReturns201WithSale` test in `ConsignmentControllerIT` (placeholder from phase 4a-2)
  - Issue a consignment, close it with installments → 201; response is `SaleResponse` with `origin = CONSIGNMENT` and correct `grossAmount`

---

## 7. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass, including cross-module listener tests
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — sale endpoints visible in OpenAPI; `POST /consignments/{id}/close` updated with new request body
