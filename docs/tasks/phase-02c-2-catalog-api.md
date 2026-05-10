# Tasks — Phase 2c-2: Catalog Write API

DTOs, write use cases, controller, and delete guards for reference data.
All product read operations live in the query module task file (phase-02c-3).

Depends on: docs/tasks/phase-02c-1-domain.md

---

## 1. DTOs

### Requests

- [ ] `CreateSkuRequest.java` — `record CreateSkuRequest(String barcode, Long sizeId, int implantationQty) {}`
- [ ] `CreateProductRequest.java` — `record CreateProductRequest(String description, BigDecimal price, BigDecimal cost, LocalDate purchaseDate, Long colorId, Long brandId, Long categoryId, List<CreateSkuRequest> skus) {}`
- [ ] `UpdateProductRequest.java` — same fields as create without the skus list (no `taxId`)
- [ ] `AddSkuRequest.java` — `record AddSkuRequest(String barcode, Long sizeId, int implantationQty) {}`

### Responses

- [ ] `ProductSkuResponse.java` — `record ProductSkuResponse(Long id, String barcode, Long sizeId, String sizeDescription, boolean active)` + `static from(ProductSku, String sizeDescription)`
- [ ] `ProductResponse.java` — full product with SKU list enriched with reference names; `static from(Product, enriched details...)` — used only in write responses (create/update)

---

## 2. Use cases

- [ ] `CreateProductUseCase`
  - Validate that colorId (optional, skip if null), brandId, and categoryId exist
  - Use `Product.builder()...build()` then iterate `request.skus()` calling `product.addSku(...)`
  - Call `repository.save(product)` explicitly so Spring Data reads `@DomainEvents`; events (`ProductCreated`, `ProductSkuCreated` × N) fire after commit

- [ ] `UpdateProductUseCase`
  - Load product → validate referenced IDs → `product.updateDetails(...)`
  - Call `repository.save(product)` explicitly to fire `ProductUpdated`

- [ ] `DeactivateProductUseCase`
  - Load product → `product.deactivate()` → `repository.save(product)`

- [ ] `AddProductSkuUseCase`
  - Load product → validate that `sizeId` exists → `product.addSku(barcode, sizeId, implantationQty)` → `repository.save(product)`

- [ ] `DeactivateProductSkuUseCase`
  - Load product → `product.deactivateSku(skuId)` → `repository.save(product)`

---

## 3. Controller

- [ ] `catalog/api/rest/ProductController.java` — **write endpoints only**
  - `POST /catalog/products` → `CreateProductUseCase`
  - `PUT /catalog/products/{id}` → `UpdateProductUseCase`
  - `DELETE /catalog/products/{id}` → `DeactivateProductUseCase`
  - `POST /catalog/products/{id}/skus` → `AddProductSkuUseCase`
  - `DELETE /catalog/products/{id}/skus/{skuId}` → `DeactivateProductSkuUseCase`

---

## 4. Reference data delete guards

With `ProductRepository` available, add existence guards to the phase-02a delete use cases:

- [ ] `DeleteColorUseCase` — inject `ProductRepository`; throw `IllegalStateException("Color is in use by one or more products")` if `existsByColorId` returns true
- [ ] `DeleteBrandUseCase` — same logic with `existsByBrandId`
- [ ] `DeleteCategoryUseCase` — same logic with `existsByCategoryId`

---

## 5. Tests

- [ ] `ProductControllerIT`
  - Create product with multiple SKUs; verify that `ProductCreated` + `ProductSkuCreated` × N are registered on the aggregate
  - Update product; verify `ProductUpdated` registered
  - Add SKU to existing product; verify `ProductSkuCreated`
  - Deactivate SKU; verify `ProductSkuDeactivated`
  - Deactivate product; verify `ProductDeactivated`
  - `400` on blank barcode, missing `brandId`, missing `categoryId`
  - `409` on duplicate barcode across distinct products
  - `422` on deactivating an already-inactive product
- [ ] `DELETE /catalog/colors/{id}` returns `422` when a product references that color

---

## 6. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — product write endpoints visible in OpenAPI
