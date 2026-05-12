# Tasks — Phase 4b: Sale Domain

Builds the `Sale` aggregate with its child entities (`SaleItem`, `SaleInstallment`, `SaleSeller`) and the `SaleConfirmed` domain event. The event carries the full snapshot required by the inventory listener (stock decrement for direct sales), the commission use case (same module), and the finance module (phase 6, receivables for deferred installments).

Depends on: docs/tasks/phase-04a-1-domain.md

---

## 1. Flyway migration

- [ ] Create `V12__sale_schema.sql`:
  ```sql
  CREATE TABLE sale (
      id                      BIGINT          NOT NULL,
      customer_id             BIGINT,
      origin                  VARCHAR(20)     NOT NULL,
      consignment_id          BIGINT,
      sale_date               DATE            NOT NULL,
      gross_amount            NUMERIC(15,2)   NOT NULL,
      store_credit_discount   NUMERIC(15,2)   NOT NULL DEFAULT 0,
      store_credit_note_id    BIGINT,
      net_amount              NUMERIC(15,2)   NOT NULL,
      created_at              TIMESTAMPTZ     NOT NULL,
      updated_at              TIMESTAMPTZ     NOT NULL,
      version                 BIGINT          NOT NULL DEFAULT 0,
      CONSTRAINT pk_sale PRIMARY KEY (id)
  );

  CREATE TABLE sale_item (
      id              BIGINT          NOT NULL,
      sale_id         BIGINT          NOT NULL,
      sku_id          BIGINT          NOT NULL,
      product_id      BIGINT          NOT NULL,
      quantity        INT             NOT NULL,
      unit_price      NUMERIC(15,2)   NOT NULL,
      item_discount   NUMERIC(15,2)   NOT NULL DEFAULT 0,
      total_price     NUMERIC(15,2)   NOT NULL,
      created_at      TIMESTAMPTZ     NOT NULL,
      updated_at      TIMESTAMPTZ     NOT NULL,
      CONSTRAINT pk_sale_item PRIMARY KEY (id),
      CONSTRAINT fk_sale_item_sale FOREIGN KEY (sale_id) REFERENCES sale(id)
  );

  CREATE TABLE sale_installment (
      id                  BIGINT          NOT NULL,
      sale_id             BIGINT          NOT NULL,
      payment_modality    VARCHAR(30)     NOT NULL,
      amount              NUMERIC(15,2)   NOT NULL,
      due_date            DATE,
      created_at          TIMESTAMPTZ     NOT NULL,
      updated_at          TIMESTAMPTZ     NOT NULL,
      CONSTRAINT pk_sale_installment PRIMARY KEY (id),
      CONSTRAINT fk_sale_installment_sale FOREIGN KEY (sale_id) REFERENCES sale(id)
  );

  CREATE TABLE sale_seller (
      sale_id             BIGINT          NOT NULL,
      salesperson_id      BIGINT          NOT NULL,
      share_percent       NUMERIC(5,2)    NOT NULL,
      commission_percent  NUMERIC(5,2)    NOT NULL,
      CONSTRAINT fk_sale_seller_sale FOREIGN KEY (sale_id) REFERENCES sale(id)
  );
  ```

---

## 2. Domain events

All in `sales/domain/event/`. Add to the existing `package-info.java` (created in phase 4a-1).

- [ ] `SaleItemSnapshot.java` — `record SaleItemSnapshot(Long skuId, Long productId, int quantity, BigDecimal unitPrice, BigDecimal itemDiscount, BigDecimal totalPrice) {}`
- [ ] `SaleInstallmentSnapshot.java` — `record SaleInstallmentSnapshot(String paymentModality, BigDecimal amount, LocalDate dueDate) {}`
- [ ] `SaleSellerSnapshot.java` — `record SaleSellerSnapshot(Long salespersonId, BigDecimal sharePercent, BigDecimal commissionPercent) {}`
- [ ] `SaleConfirmed.java`:
  ```java
  record SaleConfirmed(
      Long saleId,
      Long customerId,
      String origin,
      Long consignmentId,
      LocalDate saleDate,
      BigDecimal grossAmount,
      BigDecimal storeCreditDiscount,
      BigDecimal netAmount,
      List<SaleItemSnapshot> items,
      List<SaleInstallmentSnapshot> installments,
      List<SaleSellerSnapshot> sellers
  ) {}
  ```

---

## 3. Domain model

All in `sales/domain/model/`.

- [ ] `SaleOrigin.java` — enum `DIRECT`, `CONSIGNMENT`

- [ ] `PaymentModality.java` — enum `CASH`, `CREDIT_CARD`, `DEBIT_CARD`, `CHECK`, `PIX`, `BANK_SLIP`, `INSTALLMENT`, `STORE_ACCOUNT`
  - `public boolean isDeferred()` — returns `true` for `INSTALLMENT` and `STORE_ACCOUNT`; `false` for all others

- [ ] `SaleSeller.java` — `@Embeddable`; no `BaseEntity`
  - Fields: `salespersonId` (`@Column`), `sharePercent` (`NUMERIC(5,2)`), `commissionPercent` (`NUMERIC(5,2)`)
  - Protected no-arg constructor for JPA
  - Package-private all-args constructor used only by `Sale.create()`

- [ ] `SaleItem.java` — extends `BaseEntity`
  - Fields: `saleId`, `skuId`, `productId`, `quantity`, `unitPrice`, `itemDiscount`, `totalPrice`
  - `static SaleItem.create(Long saleId, Long skuId, Long productId, int quantity, BigDecimal unitPrice, BigDecimal itemDiscount)` — computes `totalPrice = (unitPrice.subtract(itemDiscount)).multiply(BigDecimal.valueOf(quantity))`; guards: `quantity > 0`, `unitPrice` not null and not negative, `itemDiscount` not null and not negative

- [ ] `SaleInstallment.java` — extends `BaseEntity`
  - Fields: `saleId`, `paymentModality` (`@Enumerated(EnumType.STRING)`), `amount`, `dueDate` (nullable)
  - `static SaleInstallment.create(Long saleId, PaymentModality modality, BigDecimal amount, LocalDate dueDate)` — guards: `amount` positive

- [ ] `Sale.java` — extends `BaseAggregate<Sale>`, annotated `@Version long version`
  - Fields: `customerId` (nullable), `origin` (`@Enumerated(EnumType.STRING)`), `consignmentId` (nullable), `saleDate`, `grossAmount`, `storeCreditDiscount`, `storeCreditNoteId` (nullable), `netAmount`, `sellers` (`@ElementCollection @CollectionTable(name="sale_seller")`), `items` (`@OneToMany(mappedBy="saleId", cascade=ALL, orphanRemoval=true)`), `installments` (`@OneToMany(mappedBy="saleId", cascade=ALL, orphanRemoval=true)`)
  - `static Sale.create(SaleInput input)` factory — inner record `SaleInput` holds all fields:
    - Guard: `grossAmount > 0`
    - Guard: `netAmount > 0` (= `grossAmount − storeCreditDiscount`)
    - Guard: sellers `sharePercent` sum = 100 (use `BigDecimal.compareTo`)
    - Guard: installment amounts sum = `netAmount` (tolerance: 0.01 to handle rounding)
    - Creates child entities, builds `SaleSeller` instances, registers `SaleConfirmed` via `registerEvent(toSnapshot())`
  - Private `SaleConfirmed toSnapshot()` — maps all fields and children to snapshot records

---

## 4. Repositories

In `sales/domain/repository/`.

- [ ] `SaleRepository.java` — `extends JpaRepository<Sale, Long>`
  - `Page<Sale> findAll(Specification<Sale> spec, Pageable pageable)`

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
