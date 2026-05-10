# Tasks — Phase 2c-1: Catalog Domain

Migração, eventos de domínio, entidades `Product` e `ProductSku`, e repositórios do módulo `catalog`.
Esta base é consumida pelos task files de API e query module.

Depends on: docs/tasks/phase-02a-reference-data.md

---

## 1. Flyway migration

- [x] Create `V8__catalog_product_schema.sql`:
  ```sql
  CREATE TABLE product (
      id            BIGINT         PRIMARY KEY,
      description   VARCHAR(300)   NOT NULL,
      price         NUMERIC(15,2)  NOT NULL,
      cost          NUMERIC(15,2)  NOT NULL,
      purchase_date DATE,
      color_id      BIGINT         REFERENCES color(id),
      brand_id      BIGINT         NOT NULL REFERENCES brand(id),
      category_id   BIGINT         NOT NULL REFERENCES category(id),
      active        BOOLEAN        NOT NULL DEFAULT TRUE,
      created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
      updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
  );

  CREATE TABLE product_sku (
      id         BIGINT       PRIMARY KEY,
      product_id BIGINT       NOT NULL REFERENCES product(id),
      barcode    VARCHAR(50)  NOT NULL UNIQUE,
      size_id    BIGINT       NOT NULL REFERENCES size(id),
      active     BOOLEAN      NOT NULL DEFAULT TRUE,
      created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  );
  ```
  Note: `BIGINT PRIMARY KEY` — IDs are TSID-generated in Java, no database sequence needed.

---

## 2. Domain events

Events live in `catalog/domain/event/`.
`ProductSkuCreated` is the cross-module contract with `inventory` — its shape must not change without coordinating phase 3.
All five events are also consumed by the `query` module to keep the Elasticsearch index in sync.

Events carry a `ProductSnapshot` so the `query` module can index the full state without additional DB queries (same pattern as `party`).

### Snapshot records

- [x] `ProductSkuSnapshot.java` — `record ProductSkuSnapshot(Long id, String barcode, Long sizeId, boolean active) {}`
- [x] `ProductSnapshot.java`:
  ```java
  record ProductSnapshot(
      Long id, String description, BigDecimal price, BigDecimal cost,
      LocalDate purchaseDate, Long colorId, Long brandId, Long categoryId,
      boolean active, Instant createdAt, Instant updatedAt,
      List<ProductSkuSnapshot> skus) {}
  ```

### Event records

- [x] `ProductCreated.java` — `record ProductCreated(Long productId, ProductSnapshot snapshot) {}`
- [x] `ProductUpdated.java` — `record ProductUpdated(Long productId, ProductSnapshot snapshot) {}`
- [x] `ProductDeactivated.java` — `record ProductDeactivated(Long productId, ProductSnapshot snapshot) {}`
- [x] `ProductSkuCreated.java` — `record ProductSkuCreated(Long skuId, Long productId, Long sizeId, String barcode, int implantationQty, ProductSnapshot snapshot) {}`
- [x] `ProductSkuDeactivated.java` — `record ProductSkuDeactivated(Long skuId, Long productId, ProductSnapshot snapshot) {}`

### Package

- [x] `package-info.java` — `@NamedInterface` on `catalog.domain.event` so other modules can reference these records

---

## 3. Domain model

### ProductSku (child entity — extends `BaseEntity`)

- [x] `catalog/domain/model/ProductSku.java`
  - Fields: `productId` (Long, set by aggregate), `barcode`, `sizeId` (Long — plain FK), `active`
  - `protected ProductSku() {}` for JPA
  - `static ProductSku.create(Long productId, String barcode, Long sizeId)` — package-private; guard blank barcode, non-null sizeId
  - `void deactivate()` — package-private; guard already inactive

### Product (aggregate root — extends `BaseAggregate<Product>`)

- [x] `catalog/domain/model/Product.java`
  - Fields: `description`, `price` (BigDecimal), `cost` (BigDecimal), `purchaseDate` (LocalDate), `colorId` (Long, nullable), `brandId` (Long), `categoryId` (Long), `active`
  - `@OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true)` for skus
  - `protected Product() {}` for JPA
  - `static Product.builder()` returning a static inner `Builder` — 7 parameters warrant the builder pattern (CLAUDE.md rule); `build()` guards required fields and price/cost non-negative; registers `ProductCreated(id, snapshot)`
  - `ProductSku addSku(String barcode, Long sizeId, int implantationQty)` — guard barcode unique within aggregate; registers `ProductSkuCreated` carrying `implantationQty` and snapshot
  - `void deactivateSku(Long skuId)` — finds SKU, calls `sku.deactivate()`, registers `ProductSkuDeactivated(skuId, productId, snapshot)`
  - `void updateDetails(String description, BigDecimal price, BigDecimal cost, LocalDate purchaseDate, Long colorId, Long brandId, Long categoryId)` — same guards as builder; registers `ProductUpdated(id, snapshot)`
  - `void deactivate()` — guard already inactive; registers `ProductDeactivated(id, snapshot)`
  - `private ProductSnapshot toSnapshot()` — builds snapshot from current state including skus list

---

## 4. Repositories

- [x] `catalog/domain/repository/ProductRepository.java`
  - `JpaRepository<Product, Long>`
  - `boolean existsByColorId(Long colorId)`
  - `boolean existsByBrandId(Long brandId)`
  - `boolean existsByCategoryId(Long categoryId)`
- [x] `catalog/domain/repository/ProductSkuRepository.java`
  - `JpaRepository<ProductSku, Long>`
  - `Optional<ProductSku> findByBarcode(String barcode)`

---

## 5. Verification

- [x] `./gradlew assemble` — compila sem erros
- [x] `ModularStructureTest` passa — nenhum import de sub-pacotes entre módulos
