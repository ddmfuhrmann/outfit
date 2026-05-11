# Tasks — Phase 3c-1: Stock Recount Domain

Flyway migration, `StockRecount` aggregate, `StockRecountItem` child entity, and repository.
The two-phase flow (open → close) is wired in phase-03c-2.

Depends on: docs/tasks/phase-03b-1-api.md

---

## 1. Flyway migration

- [ ] Create `V10__stock_recount_schema.sql`:
  ```sql
  CREATE TABLE stock_recount (
      id          BIGINT       PRIMARY KEY,
      date        DATE         NOT NULL,
      notes       TEXT,
      status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
      closed_at   TIMESTAMPTZ,
      created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  );

  CREATE TABLE stock_recount_item (
      id                BIGINT      PRIMARY KEY,
      stock_recount_id  BIGINT      NOT NULL REFERENCES stock_recount(id),
      product_sku_id    BIGINT      NOT NULL REFERENCES product_sku(id),
      counted_qty       INT         NOT NULL,
      system_balance    INT,
      created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (stock_recount_id, product_sku_id)
  );
  ```
  `system_balance` is `NULL` while the recount is open; set to the balance at the moment of close.
  The `UNIQUE` constraint enforces the one-SKU-per-recount domain invariant at the database level.

---

## 2. Domain model

### `StockRecountStatus` enum

- [ ] `inventory/domain/model/StockRecountStatus.java`
  - Values: `OPEN`, `CLOSED`

### `StockRecountItem` (child entity — extends `BaseEntity`)

- [ ] `inventory/domain/model/StockRecountItem.java`
  - Fields: `stockRecountId` (Long, set by aggregate), `productSkuId` (Long), `countedQty` (int), `systemBalance` (Integer, nullable)
  - `protected StockRecountItem() {}` for JPA
  - `static StockRecountItem.create(Long recountId, Long skuId, int countedQty)` — package-private; guard `countedQty >= 0`
  - `void recordSystemBalance(int balance)` — package-private; sets `systemBalance`; called during aggregate close

### `StockRecount` (aggregate root — extends `BaseAggregate<StockRecount>`)

- [ ] `inventory/domain/model/StockRecount.java`
  - Fields: `date` (LocalDate), `notes` (String nullable), `status` (StockRecountStatus), `closedAt` (Instant nullable)
  - `@OneToMany(mappedBy = "stockRecountId", cascade = CascadeType.ALL, orphanRemoval = true)` for items
  - `protected StockRecount() {}` for JPA
  - `static StockRecount.create(LocalDate date, String notes)` — sets `status = OPEN`
  - `StockRecountItem addItem(Long skuId, int countedQty)`
    - Guard: `status == OPEN`, else `IllegalStateException("Recount is already closed")`
    - Guard: no existing item for `skuId`, else `IllegalArgumentException("SKU already added to this recount")`
    - Creates item via `StockRecountItem.create(getId(), skuId, countedQty)`; adds to collection; returns item
  - `List<StockRecountItem> close()`
    - Guard: `status == CLOSED`, else `IllegalStateException("Recount is already closed")`
    - Sets `status = CLOSED`, `closedAt = Instant.now()`
    - Returns `Collections.unmodifiableList(items)` — the caller (use case) iterates to generate adjustments

---

## 3. Repository

- [ ] `inventory/domain/repository/StockRecountRepository.java`
  - `JpaRepository<StockRecount, Long>`

---

## 4. Verification

- [ ] `./gradlew assemble` — compiles without errors
- [ ] `ModularStructureTest` passes
