# Tasks — Phase 2c-1: Catalog Domain

Migração, eventos de domínio, entidades `Product` e `ProductSku`, e repositórios do módulo `catalog`.
Esta base é consumida pelos task files de API e query module.

Depends on: docs/tasks/phase-02a-reference-data.md

---

## 1. Flyway migration

- [ ] Create `V8__catalog_product_schema.sql`:
  ```sql
  CREATE TABLE product (
      id            BIGSERIAL PRIMARY KEY,
      description   VARCHAR(300)   NOT NULL,
      price         NUMERIC(15,2)  NOT NULL,
      cost          NUMERIC(15,2)  NOT NULL,
      purchase_date DATE,
      color_id      BIGINT         REFERENCES color(id),
      brand_id      BIGINT         NOT NULL REFERENCES brand(id),
      category_id   BIGINT         NOT NULL REFERENCES category(id),
      tax_id        BIGINT         NOT NULL REFERENCES tax(id),
      active        BOOLEAN        NOT NULL DEFAULT TRUE,
      created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
      updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
  );

  CREATE TABLE product_sku (
      id         BIGSERIAL PRIMARY KEY,
      product_id BIGINT      NOT NULL REFERENCES product(id),
      barcode    VARCHAR(50)  NOT NULL UNIQUE,
      size_id    BIGINT       NOT NULL REFERENCES size(id),
      active     BOOLEAN      NOT NULL DEFAULT TRUE,
      created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  );
  ```

---

## 2. Domain events

Events live in `catalog/domain/event/`.
`ProductSkuCreated` is the cross-module contract with `inventory` — its shape must not change without coordinating phase 3.
All five events are also consumed by the `query` module to keep the Elasticsearch index in sync.

- [ ] `ProductCreated.java` — `record ProductCreated(Long productId, String description) {}`
- [ ] `ProductUpdated.java` — `record ProductUpdated(Long productId) {}`
- [ ] `ProductDeactivated.java` — `record ProductDeactivated(Long productId) {}`
- [ ] `ProductSkuCreated.java` — `record ProductSkuCreated(Long skuId, Long productId, Long sizeId, String barcode, int implantationQty) {}`
- [ ] `ProductSkuDeactivated.java` — `record ProductSkuDeactivated(Long skuId, Long productId) {}`

---

## 3. Domain model

### ProductSku (child entity — extends `BaseEntity`)

- [ ] `catalog/domain/model/ProductSku.java`
  - Fields: `productId` (Long, set by aggregate), `barcode`, `sizeId` (Long — plain FK), `active`
  - `protected ProductSku() {}` for JPA
  - `static ProductSku.create(Long productId, String barcode, Long sizeId)` — guard blank barcode, non-null sizeId
  - `void deactivate()` — guard already inactive

### Product (aggregate root — extends `BaseAggregate<Product>`)

- [ ] `catalog/domain/model/Product.java`
  - Fields: `description`, `price` (BigDecimal), `cost` (BigDecimal), `purchaseDate` (LocalDate), `colorId` (Long, nullable), `brandId` (Long), `categoryId` (Long), `taxId` (Long), `active`
  - `@OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true)` for skus
  - `protected Product() {}` for JPA
  - `static Product.create(String description, BigDecimal price, BigDecimal cost, LocalDate purchaseDate, Long colorId, Long brandId, Long categoryId, Long taxId)` — guard required fields, price/cost non-negative; registers `ProductCreated`
  - `ProductSku addSku(String barcode, Long sizeId, int implantationQty)` — guard barcode unique within aggregate; registers `ProductSkuCreated` carrying `implantationQty`
  - `void deactivateSku(Long skuId)` — finds SKU, calls `sku.deactivate()`, registers `ProductSkuDeactivated`
  - `void updateDetails(String description, BigDecimal price, BigDecimal cost, LocalDate purchaseDate, Long colorId, Long brandId, Long categoryId, Long taxId)` — registers `ProductUpdated`
  - `void deactivate()` — guard already inactive; registers `ProductDeactivated`

---

## 4. Repositories

- [ ] `catalog/domain/repository/ProductRepository.java`
  - `JpaRepository<Product, Long>`
  - `boolean existsByColorId(Long colorId)`
  - `boolean existsByBrandId(Long brandId)`
  - `boolean existsByCategoryId(Long categoryId)`
  - `boolean existsByTaxId(Long taxId)`
- [ ] `catalog/domain/repository/ProductSkuRepository.java`
  - `JpaRepository<ProductSku, Long>`
  - `Optional<ProductSku> findByBarcode(String barcode)`

---

## 5. Verification

- [ ] `./gradlew assemble` — compila sem erros
- [ ] `ModularStructureTest` passa — nenhum import de sub-pacotes entre módulos
