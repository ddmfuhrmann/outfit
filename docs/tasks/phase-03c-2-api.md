# Tasks — Phase 3c-2: Stock Recount API

DTOs, use cases, controller, and integration tests for the two-phase stock recount flow.
Closing a recount delegates adjustment entries to `StockMovementService`.

Depends on: docs/tasks/phase-03c-1-domain.md

---

## 1. DTOs

- [ ] `inventory/application/dto/OpenStockRecountRequest.java`
  ```java
  record OpenStockRecountRequest(LocalDate date, String notes) {}
  ```
- [ ] `inventory/application/dto/AddRecountItemRequest.java`
  ```java
  record AddRecountItemRequest(Long skuId, int countedQty) {}
  ```
- [ ] `inventory/application/dto/RecountItemResponse.java`
  ```java
  record RecountItemResponse(Long skuId, int countedQty, Integer systemBalance, Integer delta) {
    static RecountItemResponse from(StockRecountItem item) {
      Integer delta = item.getSystemBalance() == null
          ? null
          : item.getCountedQty() - item.getSystemBalance();
      return new RecountItemResponse(item.getProductSkuId(), item.getCountedQty(), item.getSystemBalance(), delta);
    }
  }
  ```
  `systemBalance` and `delta` are `null` while the recount is open (not yet closed).

- [ ] `inventory/application/dto/StockRecountResponse.java`
  ```java
  record StockRecountResponse(
      Long id, LocalDate date, String notes,
      StockRecountStatus status, Instant closedAt,
      List<RecountItemResponse> items) {
    static StockRecountResponse from(StockRecount r) { ... }
  }
  ```

---

## 2. Use cases

- [ ] `inventory/application/usecase/OpenStockRecountUseCase.java` — `@Service @Transactional`
  - Creates `StockRecount.create(request.date(), request.notes())`
  - Guard: `request.date()` must not be null (`IllegalArgumentException`)
  - Saves and returns the id

- [ ] `inventory/application/usecase/AddRecountItemUseCase.java` — `@Service @Transactional`
  - Loads recount by id; throws `ResourceNotFoundException` if not found
  - Calls `recount.addItem(request.skuId(), request.countedQty())`
    - Domain guards propagate: closed recount → `422`, duplicate SKU → `400`
  - Saves recount (dirty checking — no explicit `save` needed since item is in cascade)

- [ ] `inventory/application/usecase/CloseStockRecountUseCase.java` — `@Service @Transactional`
  - Loads recount; calls `recount.close()` (propagates `IllegalStateException` if already closed → `422`)
  - For each item in the returned list:
    1. Read `StockBalance` for `item.getProductSkuId()` — skip item if balance not found (SKU unknown to inventory)
    2. Set `item.recordSystemBalance(balance.getCurrentBalance())`
    3. Compute `delta = item.getCountedQty() - balance.getCurrentBalance()`
    4. If `delta != 0`: call `StockMovementService.record(skuId, productId, delta, RECOUNT_ADJUSTMENT, recountId, Instant.now())`
       - `productId` resolved the same way as in `RecordManualAdjustmentUseCase` (query latest entry for skuId)
  - Saves recount (`repository.save(recount)`) to persist `status`, `closedAt`, and `systemBalance` on items

- [ ] `inventory/application/usecase/GetStockRecountUseCase.java` — `@Service`
  - Loads recount with items; returns `StockRecountResponse.from(recount)`

---

## 3. Controller

- [ ] `inventory/api/rest/StockRecountController.java`
  - `POST /inventory/recount` → `OpenStockRecountUseCase`; returns `201 Created` with `{ "id": ... }`
  - `POST /inventory/recount/{id}/items` → `AddRecountItemUseCase`; returns `204 No Content`
  - `POST /inventory/recount/{id}/close` → `CloseStockRecountUseCase`; returns `204 No Content`
  - `GET /inventory/recount/{id}` → `GetStockRecountUseCase`; returns `200` with `StockRecountResponse`

---

## 4. Tests

- [ ] `StockRecountControllerIT`
  - Setup: product + two SKUs with `implantationQty = 20` and `implantationQty = 5`
  - Open recount → `201`; status is `OPEN`
  - Add both SKUs → `204` each
  - Add same SKU twice → `400`
  - `GET /inventory/recount/{id}` while open → items have `systemBalance = null`, `delta = null`
  - Close recount where SKU A counted 18 (balance 20) and SKU B counted 5 (balance 5):
    - `StockEntry` for SKU A: `quantity = -2`, `source = RECOUNT_ADJUSTMENT`, `runningBalance = 18`
    - No `StockEntry` for SKU B (delta = 0)
    - `StockBalance.currentBalance = 18` for SKU A
  - `GET /inventory/recount/{id}` after close → `systemBalance = 20`, `delta = -2` for SKU A; `systemBalance = 5`, `delta = 0` for SKU B
  - Close already-closed recount → `422`
  - Add item to closed recount → `422`
  - `StockEntryRecorded` published for SKU A; not published for SKU B

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — all four recount endpoints visible in OpenAPI
