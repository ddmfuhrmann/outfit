# Tasks — Phase 4c: Store Credit Note API

Implements the store credit note use cases, REST controller, the inventory listener for stock increment on returns, and updates `CreateSaleUseCase` to handle store credit note consumption.

Depends on: docs/tasks/phase-04c-1-domain.md, docs/tasks/phase-04b-2-api.md

---

## 1. DTOs

In `sales/application/dto/`.

- [ ] `StoreCreditItemRequest.java` — `record StoreCreditItemRequest(@NotNull Long skuId, @NotNull Long productId, @Positive int quantity, @NotNull @Positive BigDecimal unitValue) {}`
- [ ] `CreateStoreCreditNoteRequest.java` — `record CreateStoreCreditNoteRequest(@NotNull Long customerId, @NotEmpty List<StoreCreditItemRequest> items) {}`
- [ ] `StoreCreditItemResponse.java` — `record StoreCreditItemResponse(Long id, Long skuId, Long productId, int quantity, BigDecimal unitValue, BigDecimal totalValue)` with `static from(StoreCreditItem)`
- [ ] `StoreCreditNoteResponse.java` — `record StoreCreditNoteResponse(Long id, Long customerId, String status, BigDecimal totalValue, Long saleId, Instant consumedAt, List<StoreCreditItemResponse> items)` with `static from(StoreCreditNote)`

---

## 2. Use cases

In `sales/application/usecase/`.

- [ ] `CreateStoreCreditNoteUseCase.java` — `@Service @Transactional`
  - Builds `List<StoreCreditNote.StoreCreditItemInput>` from request items
  - Calls `StoreCreditNote.create(customerId, inputs)`, then `repository.save(note)` explicitly (fires `StoreCreditNoteCreated`)
  - Logs `INFO` with note ID and total value
  - Returns `StoreCreditNoteResponse.from(note)`

- [ ] `GetStoreCreditNoteUseCase.java` — `@Service` (no `@Transactional`)
  - Loads by ID; throws `EntityNotFoundException` if absent; returns `StoreCreditNoteResponse.from(note)`

- [ ] `ListStoreCreditNotesUseCase.java` — `@Service` (no `@Transactional`)
  - Filters: `customerId`, `status` (parsed to `StoreCreditNoteStatus`), `Pageable`
  - Returns `PageResponse<StoreCreditNoteResponse>`

---

## 3. `CreateSaleUseCase` update

In `sales/application/usecase/CreateSaleUseCase.java` (created in phase 4b-2).

- [ ] Inject `StoreCreditNoteRepository` into `CreateSaleUseCase`
- [ ] When `request.storeCreditNoteId()` is non-null:
  1. Load `StoreCreditNote`; throw `EntityNotFoundException` if absent
  2. Guard: `note.getStatus() == CONSUMED` → `IllegalStateException("store credit note is already consumed")`
  3. Guard: `note.getCustomerId()` ≠ `request.customerId()` → `IllegalArgumentException("store credit note does not belong to this customer")`
  4. Set `storeCreditDiscount = note.getTotalValue()` in the sale input
  5. After `saleRepository.save(sale)`: call `note.consume(sale.getId())`, then `storeCreditNoteRepository.save(note)` explicitly (fires `StoreCreditNoteConsumed`)
- [ ] Update `netAmount` computation: `netAmount = grossAmount − storeCreditDiscount`

---

## 4. Inventory listener: credit note stock increment

In `inventory/application/`.

- [ ] `RecordCreditReturnStockUseCase.java` — `@Service @Transactional`
  - `void execute(Long storeCreditNoteId, List<StoreCreditItemSnapshot> items)`
  - For each item: calls `StockMovementService.recordEntry(item.skuId(), item.productId(), +item.quantity(), StockSource.RETURN, storeCreditNoteId, Instant.now())`

- [ ] `StoreCreditNoteCreatedListener.java` — `@Component` in `inventory/application/listener/`
  - `@ApplicationModuleListener void on(StoreCreditNoteCreated event)` → delegates to `RecordCreditReturnStockUseCase`

---

## 5. Controller

In `sales/api/rest/`.

- [ ] `StoreCreditNoteController.java` — `@RestController @RequestMapping("/store-credit-notes")`
  - `POST /store-credit-notes` → `CreateStoreCreditNoteUseCase` → `201 Created` + `Location: /store-credit-notes/{id}`
  - `GET /store-credit-notes/{id}` → `GetStoreCreditNoteUseCase` → `200 OK`
  - `GET /store-credit-notes` → `ListStoreCreditNotesUseCase` (query params: `customerId`, `status`, `Pageable`) → `200 OK`
  - OpenAPI `@Operation` on each method

---

## 6. Integration tests

In `src/test/java/.../outfit/sales/StoreCreditNoteControllerIT.java` — extends `AbstractIT`.

- [ ] `createStoreCreditNoteReturns201` — valid request → 201; response has correct `totalValue` (sum of item totals) and `status = OPEN`
- [ ] `createStoreCreditNoteIncrementsStock` — after create, assert stock movement entry with source RETURN for each SKU in inventory
- [ ] `listStoreCreditNotesByCustomerAndStatus` — create two notes for different customers; filter by `customerId` and `status=OPEN` → only matching note returned
- [ ] `createSaleWithStoreCreditNoteAppliesFullDiscount` — create a note, then create a sale referencing it → `storeCreditDiscount = note.totalValue`; note `status = CONSUMED`
- [ ] `createSaleWithConsumedNoteReturns422` — consume a note, then attempt to use it in another sale → 422
- [ ] `createSaleWithNoteFromOtherCustomerReturns422` — note belongs to customer A, sale is for customer B → 422

---

## 7. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — store credit note endpoints visible in OpenAPI
