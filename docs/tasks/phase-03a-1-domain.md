# Tasks — Phase 3a-1: Inventory Domain

Bootstraps the inventory module: Flyway migration, ledger entities, domain event, repositories,
internal movement service, and the `ProductSkuCreated` listener that seeds the initial stock.

Depends on: docs/tasks/phase-02c-2-catalog-api.md

---

## 1. Flyway migration

- [ ] Create `V9__inventory_schema.sql`:
  ```sql
  CREATE TABLE stock_balance (
      product_sku_id  BIGINT      PRIMARY KEY REFERENCES product_sku(id),
      current_balance INT         NOT NULL DEFAULT 0,
      version         BIGINT      NOT NULL DEFAULT 0,
      created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );

  CREATE TABLE stock_entry (
      id              BIGINT       PRIMARY KEY,
      product_sku_id  BIGINT       NOT NULL REFERENCES product_sku(id),
      product_id      BIGINT       NOT NULL REFERENCES product(id),
      quantity        INT          NOT NULL,
      running_balance INT          NOT NULL,
      source          VARCHAR(50)  NOT NULL,
      source_key      BIGINT,
      occurred_at     TIMESTAMPTZ  NOT NULL,
      created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  );

  CREATE INDEX idx_stock_entry_sku_occurred
      ON stock_entry(product_sku_id, occurred_at DESC);
  ```
  `stock_balance.product_sku_id` is the natural PK — no TSID sequence needed.
  `stock_entry.id` is TSID-generated in Java.

---

## 2. Domain event

- [ ] `inventory/domain/event/StockEntryRecorded.java`
  ```java
  public record StockEntryRecorded(
      Long entryId, Long skuId, Long productId,
      int quantity, int runningBalance,
      StockSource source, Long sourceKey,
      Instant occurredAt) {}
  ```
- [ ] `inventory/domain/event/package-info.java` — `@NamedInterface` so the `query` module can import this record

---

## 3. Domain model

### `StockSource` enum

- [ ] `inventory/domain/model/StockSource.java`
  - Values: `INITIAL_STOCK`, `MANUAL_ADJUSTMENT`, `RECOUNT_ADJUSTMENT`, `SALE`, `CONSIGNMENT`, `PURCHASE`
  - `SALE`, `CONSIGNMENT`, `PURCHASE` are reserved for later phases; defined here so the schema is stable

### `StockBalance` (standalone entity — does **not** extend `BaseEntity`)

- [ ] `inventory/domain/model/StockBalance.java`
  - `@Entity @Table(name = "stock_balance")`
  - `@Id Long productSkuId` — set explicitly from the SKU id; no TSID generation
  - `@Version Long version` — optimistic locking; prevents two concurrent movements from computing the same `runningBalance`
  - `int currentBalance`
  - `protected StockBalance() {}` for JPA
  - `static StockBalance.create(Long skuId)` — sets `productSkuId`, `currentBalance = 0`
  - `void apply(int quantity)` — `currentBalance += quantity`; called after the `StockEntry` is created

### `StockEntry` (aggregate root — extends `BaseAggregate<StockEntry>`)

- [ ] `inventory/domain/model/StockEntry.java`
  - Fields: `productSkuId` (Long), `productId` (Long), `quantity` (int), `runningBalance` (int), `source` (StockSource), `sourceKey` (Long nullable), `occurredAt` (Instant)
  - `protected StockEntry() {}` for JPA
  - `static StockEntry.create(Long skuId, Long productId, int quantity, int currentBalance, StockSource source, Long sourceKey, Instant occurredAt)`
    - Computes `runningBalance = currentBalance + quantity`
    - Registers `StockEntryRecorded(getId(), skuId, productId, quantity, runningBalance, source, sourceKey, occurredAt)`

---

## 4. Repositories

- [ ] `inventory/domain/repository/StockBalanceRepository.java`
  - `JpaRepository<StockBalance, Long>`
  - `@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("SELECT b FROM StockBalance b WHERE b.productSkuId = :skuId") Optional<StockBalance> findAndLock(@Param("skuId") Long skuId)`
- [ ] `inventory/domain/repository/StockEntryRepository.java`
  - `JpaRepository<StockEntry, Long>`
  - `Page<StockEntry> findByProductSkuIdOrderByOccurredAtDesc(Long skuId, Pageable pageable)`

---

## 5. Internal movement service

- [ ] `inventory/application/StockMovementService.java` — `@Service @Transactional`
  - Single method: `StockEntry record(Long skuId, Long productId, int quantity, StockSource source, Long sourceKey, Instant occurredAt)`
  - Acquires pessimistic write lock on `StockBalance` via `findAndLock`; if balance does not exist yet, creates it via `StockBalance.create(skuId)`
  - Creates `StockEntry.create(...)` with the locked `balance.getCurrentBalance()`
  - Calls `entryRepository.save(entry)` to register `@DomainEvents`
  - Calls `balance.apply(quantity)` then `balanceRepository.save(balance)`
  - Returns the saved entry

---

## 6. Initial stock use case and listener

- [ ] `inventory/application/usecase/RecordInitialStockUseCase.java` — `@Service @Transactional`
  - Receives `Long skuId, Long productId, int implantationQty, Instant occurredAt`
  - Always calls `StockMovementService.record(...)` with `source = INITIAL_STOCK`, `sourceKey = skuId`, `quantity = implantationQty`
  - If `implantationQty == 0`: creates the `StockBalance` only — no `StockEntry`
    - Directly saves `StockBalance.create(skuId)` without calling `StockMovementService`
  - Guard: throws `IllegalArgumentException` if `implantationQty < 0`

- [ ] `inventory/application/listener/ProductSkuCreatedListener.java` — `@ApplicationModuleListener`
  - Handles `catalog.domain.event.ProductSkuCreated`
  - Delegates to `RecordInitialStockUseCase.execute(event.skuId(), event.productId(), event.implantationQty(), Instant.now())`
  - Not `@Transactional` — use case owns the transaction

---

## 7. Integration test

- [ ] `ProductSkuCreatedListenerIT`
  - Create a product with two SKUs via write API; verify `StockBalance` rows exist for both
  - SKU with `implantationQty = 10` → `StockEntry(source=INITIAL_STOCK, quantity=10, runningBalance=10)` exists; `StockBalance.currentBalance = 10`
  - SKU with `implantationQty = 0` → no `StockEntry`; `StockBalance.currentBalance = 0`
  - Verify `StockEntryRecorded` is published only for the positive qty case

---

## 8. Verification

- [ ] `./gradlew assemble` — compiles without errors
- [ ] `./gradlew test` — IT green
- [ ] `ModularStructureTest` passes — listener imports only `catalog.domain.event.ProductSkuCreated`
