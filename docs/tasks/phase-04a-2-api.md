# Tasks — Phase 4a: Consignment API

Implements the write-side use cases, REST controller, and the two inventory listeners that handle stock movements triggered by consignment events. The close endpoint creates the sale directly inside `CloseConsignmentUseCase` — no separate sale listener is needed.

GET endpoints (list and detail) are Elasticsearch-backed and live in the query module — see `docs/tasks/phase-04a-3-query-module.md`.

Depends on: docs/tasks/phase-04a-1-domain.md

---

## 1. DTOs

In `sales/application/dto/`.

- [ ] `ConsignmentItemRequest.java` — `record ConsignmentItemRequest(@NotNull Long skuId, @NotNull Long productId, @Positive int quantity, @NotNull @Positive BigDecimal unitPrice) {}`
- [ ] `IssueConsignmentRequest.java` — `record IssueConsignmentRequest(@NotNull Long customerId, @NotEmpty List<Long> salespersonIds, @NotNull LocalDate issueDate, String notes, @NotEmpty List<ConsignmentItemRequest> items) {}`
- [ ] `ReturnItemRequest.java` — `record ReturnItemRequest(@NotNull Long skuId, @Positive int quantityReturned) {}`
- [ ] `ReturnItemsRequest.java` — `record ReturnItemsRequest(@NotEmpty List<ReturnItemRequest> items) {}`
- [ ] `CloseConsignmentRequest.java` — `record CloseConsignmentRequest(@NotEmpty List<Long> sellerIds, @NotEmpty List<CreateSaleInstallmentRequest> installments) {}`
  - References `CreateSaleInstallmentRequest` from phase 4b; add it as a forward dependency — this DTO can be written now as a stub and completed in phase 4b-2
- [ ] `ConsignmentItemResponse.java` — `record ConsignmentItemResponse(Long id, Long skuId, Long productId, BigDecimal unitPrice, int quantityIssued, int quantityReturned, int quantitySold)` with `static ConsignmentItemResponse from(ConsignmentItem item)`
- [ ] `ConsignmentResponse.java` — `record ConsignmentResponse(Long id, Long customerId, List<Long> salespersonIds, String status, LocalDate issueDate, Instant closedAt, String notes, List<ConsignmentItemResponse> items)` with `static ConsignmentResponse from(Consignment c)`

---

## 2. Use cases

In `sales/application/usecase/`.

- [ ] `IssueConsignmentUseCase.java` — `@Service @Transactional`
  - Builds `Consignment.ConsignmentItemInput` list from request items
  - Calls `Consignment.create(...)`, then `repository.save(consignment)` explicitly (required for `@DomainEvents` to fire)
  - Logs `INFO` after save
  - Returns `ConsignmentResponse.from(consignment)`

- [ ] `ReturnConsignmentItemsUseCase.java` — `@Service @Transactional`
  - Loads consignment; throws `EntityNotFoundException` (→ 404) if absent
  - Builds `Map<Long, Integer>` from request items
  - Calls `consignment.returnItems(map)`, then `repository.save(consignment)`
  - Returns `ConsignmentResponse.from(consignment)`

- [ ] `CloseConsignmentUseCase.java` — `@Service @Transactional`
  - Loads consignment; throws `EntityNotFoundException` (→ 404) if absent
  - Calls `consignment.close()`, then `repository.save(consignment)` — fires `ConsignmentClosed`
  - Delegates to `CreateSaleUseCase` (injected) using the sold items from the closed consignment and the installments + sellerIds from the request
    - `origin = CONSIGNMENT`, `consignmentId = consignment.getId()`
    - `grossAmount` = sum of `(item.unitPrice × item.quantitySold)` for each sold item
    - `storeCreditDiscount = BigDecimal.ZERO` (no store credit at this stage)
  - Returns the `SaleResponse` from `CreateSaleUseCase`
  - Note: `CreateSaleUseCase` is defined in phase 4b-2; this use case will be completed in that phase. Leave injection as a placeholder until then.

---

## 3. Controller

In `sales/api/rest/`.

- [ ] `ConsignmentController.java` — `@RestController @RequestMapping("/consignments")`
  - `POST /consignments` → `IssueConsignmentUseCase` → `201 Created` + `Location` header
  - `POST /consignments/{id}/return-items` → `ReturnConsignmentItemsUseCase` → `200 OK`
  - `POST /consignments/{id}/close` → `CloseConsignmentUseCase` → `201 Created` (returns `SaleResponse`)
  - OpenAPI `@Operation` on each method
  - GETs live in `query/api/rest/ConsignmentQueryController` (phase 04a-3)

---

## 4. Inventory module additions

In `inventory/application/`. These classes live in the `inventory` package and import only from `sales.domain.event` (the `@NamedInterface`).

- [ ] `RecordConsignmentIssueStockUseCase.java` — `@Service @Transactional`
  - `void execute(Long consignmentId, List<ConsignmentItemSnapshot> items)`
  - For each item: calls `StockMovementService.recordEntry(item.skuId(), item.productId(), -item.quantity(), StockSource.CONSIGNMENT, consignmentId, Instant.now())`

- [ ] `ConsignmentIssuedListener.java` — `@Component`
  - `@ApplicationModuleListener void on(ConsignmentIssued event)` → delegates to `RecordConsignmentIssueStockUseCase`

- [ ] `RecordConsignmentReturnStockUseCase.java` — `@Service @Transactional`
  - `void execute(Long consignmentId, List<ConsignmentItemSnapshot> returnedItems)`
  - For each item: calls `StockMovementService.recordEntry(item.skuId(), item.productId(), +item.quantity(), StockSource.CONSIGNMENT, consignmentId, Instant.now())`

- [ ] `ConsignmentItemsReturnedListener.java` — `@Component`
  - `@ApplicationModuleListener void on(ConsignmentItemsReturned event)` → delegates to `RecordConsignmentReturnStockUseCase`

---

## 5. Integration tests

In `src/test/java/.../outfit/sales/ConsignmentControllerIT.java` — extends `AbstractIT`.

- [ ] `issueConsignmentReturns201` — valid request → 201; verify `quantityIssued` on each item
- [ ] `issueConsignmentDecrementsStockPerItem` — after issue, call `GET /inventory/movements/{skuId}` and assert a CONSIGNMENT decrement entry exists for each SKU
- [ ] `returnItemsUpdatesQuantityReturned` — issue then return 1 unit → 200; `quantityReturned = 1`, `quantitySold = quantityIssued - 1`
- [ ] `returnItemsIncrementsStock` — after return, verify stock increment entry (source CONSIGNMENT) in inventory
- [ ] `returnMoreThanIssuedReturns422` — exceed issued quantity → 422
- [ ] `returnItemsOnClosedConsignmentReturns422` — close then attempt return → 422
- [ ] `closeConsignmentReturns201WithSale` — issue, then close with installments → 201; response is a `SaleResponse` with `origin = CONSIGNMENT`
  - Note: complete this test in phase 4b-2 once `CreateSaleUseCase` is available
- [ ] `closeAlreadyClosedConsignmentReturns422` — close twice → 422

---

## 6. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — consignment endpoints visible in OpenAPI
