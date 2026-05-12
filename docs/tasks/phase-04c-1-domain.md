# Tasks — Phase 4c: Store Credit Note Domain

Builds the `StoreCreditNote` aggregate and its events. Adds `StockSource.RETURN` to the inventory module. When a note is created, `StoreCreditNoteCreated` triggers an inventory stock increment for each returned item. When the note is consumed by a sale, `StoreCreditNoteConsumed` is published for auditability.

Depends on: docs/tasks/phase-04b-1-domain.md

---

## 1. Inventory module: `StockSource.RETURN`

- [ ] Add `RETURN` to the existing `StockSource` enum in `inventory/domain/model/StockSource.java`
  - No Flyway migration needed — the value is stored as `VARCHAR` and the new value is not yet in the database

---

## 2. Flyway migration

- [ ] Create `V13__store_credit_note_schema.sql`:
  ```sql
  CREATE TABLE store_credit_note (
      id              BIGINT          NOT NULL,
      customer_id     BIGINT          NOT NULL,
      status          VARCHAR(20)     NOT NULL,
      total_value     NUMERIC(15,2)   NOT NULL,
      sale_id         BIGINT,
      consumed_at     TIMESTAMPTZ,
      created_at      TIMESTAMPTZ     NOT NULL,
      updated_at      TIMESTAMPTZ     NOT NULL,
      CONSTRAINT pk_store_credit_note PRIMARY KEY (id)
  );

  CREATE TABLE store_credit_item (
      id                      BIGINT          NOT NULL,
      store_credit_note_id    BIGINT          NOT NULL,
      sku_id                  BIGINT          NOT NULL,
      product_id              BIGINT          NOT NULL,
      quantity                INT             NOT NULL,
      unit_value              NUMERIC(15,2)   NOT NULL,
      total_value             NUMERIC(15,2)   NOT NULL,
      created_at              TIMESTAMPTZ     NOT NULL,
      updated_at              TIMESTAMPTZ     NOT NULL,
      CONSTRAINT pk_store_credit_item PRIMARY KEY (id),
      CONSTRAINT fk_store_credit_item_note FOREIGN KEY (store_credit_note_id) REFERENCES store_credit_note(id)
  );
  ```

---

## 3. Domain events

All in `sales/domain/event/`. Add to the existing `package-info.java`.

- [ ] `StoreCreditItemSnapshot.java` — `record StoreCreditItemSnapshot(Long skuId, Long productId, int quantity, BigDecimal unitValue) {}`
- [ ] `StoreCreditNoteCreated.java` — `record StoreCreditNoteCreated(Long storeCreditNoteId, Long customerId, List<StoreCreditItemSnapshot> items, BigDecimal totalValue, Instant createdAt) {}`
  - Consumed by inventory to increment stock per item (source `RETURN`)
- [ ] `StoreCreditNoteConsumed.java` — `record StoreCreditNoteConsumed(Long storeCreditNoteId, Long customerId, Long saleId, BigDecimal totalValue, Instant consumedAt) {}`

---

## 4. Domain model

All in `sales/domain/model/`.

- [ ] `StoreCreditNoteStatus.java` — enum `OPEN`, `CONSUMED`

- [ ] `StoreCreditItem.java` — extends `BaseEntity`
  - Fields: `storeCreditNoteId`, `skuId`, `productId`, `quantity`, `unitValue`, `totalValue`
  - `static StoreCreditItem.create(Long storeCreditNoteId, Long skuId, Long productId, int quantity, BigDecimal unitValue)` — computes `totalValue = unitValue.multiply(BigDecimal.valueOf(quantity))`; guards: `quantity > 0`, `unitValue` not null and not negative

- [ ] `StoreCreditNote.java` — extends `BaseAggregate<StoreCreditNote>`
  - Fields: `customerId`, `status` (`@Enumerated(EnumType.STRING)`), `totalValue`, `saleId` (nullable), `consumedAt` (nullable), `items` (`@OneToMany(mappedBy="storeCreditNoteId", cascade=ALL, orphanRemoval=true)`)
  - `static StoreCreditNote.create(Long customerId, List<StoreCreditItemInput> itemInputs)` — inner record `StoreCreditItemInput(Long skuId, Long productId, int quantity, BigDecimal unitValue)`; guard: `itemInputs` not null and not empty; creates items via `StoreCreditItem.create()`; computes `totalValue` = sum of item `totalValue`s; sets `status = OPEN`; registers `StoreCreditNoteCreated`
  - `void consume(Long saleId)` — guard: `status == CONSUMED` → `IllegalStateException("store credit note is already consumed")`; sets `status = CONSUMED`, `this.saleId = saleId`, `consumedAt = Instant.now()`; registers `StoreCreditNoteConsumed`

---

## 5. Repositories

In `sales/domain/repository/`.

- [ ] `StoreCreditNoteRepository.java` — `extends JpaRepository<StoreCreditNote, Long>`
  - `Page<StoreCreditNote> findAll(Specification<StoreCreditNote> spec, Pageable pageable)`

---

## 6. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
