# Tasks — Phase 3b-1: Stock Control API

Write-side use cases and controller for manual stock adjustments and movement history.
All writes go through `StockMovementService` established in phase 3a.

Depends on: docs/tasks/phase-03a-1-domain.md

---

## 1. DTOs

- [ ] `inventory/application/dto/ManualAdjustmentRequest.java`
  ```java
  record ManualAdjustmentRequest(Long skuId, int desiredBalance, Instant occurredAt) {}
  ```
- [ ] `inventory/application/dto/StockMovementResponse.java`
  ```java
  record StockMovementResponse(
      Long id, int quantity, int runningBalance,
      StockSource source, Long sourceKey, Instant occurredAt) {
    static StockMovementResponse from(StockEntry e) { ... }
  }
  ```

---

## 2. Use cases

- [ ] `inventory/application/usecase/RecordManualAdjustmentUseCase.java` — `@Service @Transactional`
  - Load `StockBalance` for `skuId`; throw `ResourceNotFoundException` if not found
    - A balance not found means no SKU exists in inventory — do not create one here
  - Compute `quantity = request.desiredBalance() - balance.getCurrentBalance()`
  - Throw `IllegalArgumentException("Desired balance equals current balance")` if `quantity == 0`
  - Delegate to `StockMovementService.record(skuId, productId, quantity, MANUAL_ADJUSTMENT, null, request.occurredAt())`
    - `productId` is not on `StockBalance`; load it from `StockEntryRepository` via a query on the most recent entry for the SKU
      - Add `Optional<Long> findProductIdByProductSkuId(Long skuId)` to `StockEntryRepository` (`@Query("SELECT e.productId FROM StockEntry e WHERE e.productSkuId = :skuId ORDER BY e.occurredAt DESC LIMIT 1")`)

- [ ] `inventory/application/usecase/GetStockMovementsUseCase.java` — `@Service`
  - Accepts `Long skuId`, `Pageable`
  - Returns `PageResponse<StockMovementResponse>` from `StockEntryRepository.findByProductSkuIdOrderByOccurredAtDesc`
  - Does not validate SKU existence — an empty page is a valid response for an unknown SKU

---

## 3. Controller

- [ ] `inventory/api/rest/InventoryController.java`
  - `POST /inventory/adjustment` → `RecordManualAdjustmentUseCase`; returns `204 No Content`
  - `GET /inventory/movements/{skuId}?page=&size=` → `GetStockMovementsUseCase`; returns `200` with `PageResponse<StockMovementResponse>`
  - OpenAPI: note that `/inventory/movements/{skuId}` is served from PostgreSQL

---

## 4. Tests

- [ ] `InventoryControllerIT`
  - Setup: create product + SKU with `implantationQty = 20` via catalog write API
  - `POST /inventory/adjustment` with `desiredBalance = 25` → `204`; `StockBalance.currentBalance = 25`; new `StockEntry(source=MANUAL_ADJUSTMENT, quantity=5, runningBalance=25)` exists
  - `POST /inventory/adjustment` with `desiredBalance = 25` again (same as current) → `422`
  - `POST /inventory/adjustment` for unknown SKU → `404`
  - `GET /inventory/movements/{skuId}` → returns two entries (initial + adjustment) ordered by `occurredAt` descending
  - `StockEntryRecorded` is published after a successful adjustment

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — `POST /inventory/adjustment` and `GET /inventory/movements/{skuId}` visible in OpenAPI
