# Tasks — Phase 4a: Consignment Domain

Builds the `Consignment` aggregate and its domain events. All stock side effects are event-driven: `ConsignmentIssued` triggers an inventory decrement and `ConsignmentItemsReturned` triggers an increment. `ConsignmentClosed` is consumed by the sales use case in phase 4a-2 to create the resulting sale.

Depends on: none

---

## 1. Flyway migration

- [ ] Create `V11__consignment_schema.sql`:
  ```sql
  CREATE TABLE consignment (
      id                BIGINT          NOT NULL,
      customer_id       BIGINT          NOT NULL,
      status            VARCHAR(20)     NOT NULL,
      issue_date        DATE            NOT NULL,
      closed_at         TIMESTAMPTZ,
      notes             VARCHAR(500),
      created_at        TIMESTAMPTZ     NOT NULL,
      updated_at        TIMESTAMPTZ     NOT NULL,
      version           BIGINT          NOT NULL DEFAULT 0,
      CONSTRAINT pk_consignment PRIMARY KEY (id)
  );

  CREATE TABLE consignment_item (
      id                  BIGINT          NOT NULL,
      consignment_id      BIGINT          NOT NULL,
      sku_id              BIGINT          NOT NULL,
      product_id          BIGINT          NOT NULL,
      unit_price          NUMERIC(15,2)   NOT NULL,
      quantity_issued     INT             NOT NULL,
      quantity_returned   INT             NOT NULL DEFAULT 0,
      created_at          TIMESTAMPTZ     NOT NULL,
      updated_at          TIMESTAMPTZ     NOT NULL,
      CONSTRAINT pk_consignment_item PRIMARY KEY (id),
      CONSTRAINT fk_consignment_item_consignment FOREIGN KEY (consignment_id) REFERENCES consignment(id)
  );

  CREATE TABLE consignment_salesperson (
      consignment_id    BIGINT  NOT NULL,
      salesperson_id    BIGINT  NOT NULL,
      CONSTRAINT fk_consignment_salesperson FOREIGN KEY (consignment_id) REFERENCES consignment(id)
  );
  ```

---

## 2. Domain events

All in `sales/domain/event/`. Add `package-info.java` with `@NamedInterface` so inventory and sales listeners can import these records across module boundaries.

- [ ] `ConsignmentItemSnapshot.java` — `record ConsignmentItemSnapshot(Long skuId, Long productId, int quantity, BigDecimal unitPrice) {}`
- [ ] `ConsignmentIssued.java` — `record ConsignmentIssued(Long consignmentId, Long customerId, List<Long> salespersonIds, List<ConsignmentItemSnapshot> items) {}`
  - Consumed by inventory to decrement stock per item (source `CONSIGNMENT`)
- [ ] `ConsignmentItemsReturned.java` — `record ConsignmentItemsReturned(Long consignmentId, List<ConsignmentItemSnapshot> returnedItems) {}`
  - Consumed by inventory to increment stock per returned item (source `CONSIGNMENT`)
- [ ] `ConsignmentClosed.java` — `record ConsignmentClosed(Long consignmentId, Long customerId, List<Long> salespersonIds, LocalDate issueDate, Instant closedAt, List<ConsignmentItemSnapshot> soldItems) {}`
  - `soldItems` contains only items where `quantitySold > 0`; consumed by `CloseConsignmentUseCase` (phase 4a-2) to create the sale directly
- [ ] `package-info.java` — `@NamedInterface` on the `sales.domain.event` package

---

## 3. Domain model

All in `sales/domain/model/`.

- [ ] `ConsignmentStatus.java` — enum `OPEN`, `CLOSED`

- [ ] `ConsignmentItem.java` — extends `BaseEntity`
  - Fields: `consignmentId` (`@Column`), `skuId`, `productId`, `unitPrice` (`NUMERIC(15,2)`), `quantityIssued`, `quantityReturned`
  - `static ConsignmentItem.create(Long consignmentId, Long skuId, Long productId, int quantity, BigDecimal unitPrice)` — guards: `quantity > 0`, `unitPrice` not null and not negative
  - `void recordReturn(int quantity)` — guard: `quantity <= 0` → `IllegalArgumentException("quantity must be positive")`; `quantityReturned + quantity > quantityIssued` → `IllegalArgumentException("return exceeds issued quantity")`; adds to `quantityReturned`
  - `int getQuantitySold()` — `return quantityIssued - quantityReturned;`

- [ ] `Consignment.java` — extends `BaseAggregate<Consignment>`, `@Version long version`
  - Fields: `customerId`, `salespersonIds` (`@ElementCollection @CollectionTable(name="consignment_salesperson")`), `status` (`@Enumerated(EnumType.STRING)`), `issueDate`, `closedAt`, `notes`, `items` (`@OneToMany(mappedBy="consignmentId", cascade=ALL, orphanRemoval=true)`)
  - `static Consignment.create(Long customerId, List<Long> salespersonIds, LocalDate issueDate, String notes, List<ConsignmentItemInput> itemInputs)` — guards: `customerId` not null; `salespersonIds` not null and not empty; `issueDate` not null; `itemInputs` not null and not empty; creates items via `ConsignmentItem.create()`; registers `ConsignmentIssued`
  - `void returnItems(Map<Long, Integer> quantitiesBySkuId)` — guard: `status != OPEN` → `IllegalStateException("consignment is already closed")`; for each entry, find the matching item by `skuId` (throw `IllegalArgumentException` if not found), call `item.recordReturn(qty)`; registers `ConsignmentItemsReturned` carrying only the items present in the map
  - `void close()` — guard: `status != OPEN` → `IllegalStateException("consignment is already closed")`; sets `status = CLOSED`, `closedAt = Instant.now()`; registers `ConsignmentClosed` with `soldItems` = items where `getQuantitySold() > 0`
  - Inner record `ConsignmentItemInput(Long skuId, Long productId, int quantity, BigDecimal unitPrice)` — used only in the factory method

---

## 4. Repositories

In `sales/domain/repository/`.

- [ ] `ConsignmentRepository.java` — `extends JpaRepository<Consignment, Long>`
  - `Page<Consignment> findAll(Specification<Consignment> spec, Pageable pageable)`

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
