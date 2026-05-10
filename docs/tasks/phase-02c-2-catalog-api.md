# Tasks — Phase 2c-2: Catalog Write API

DTOs, use cases de escrita, controller e guards de deleção de dados de referência.
Todas as operações de leitura de produto ficam no task file do query module (phase-02c-3).

Depends on: docs/tasks/phase-02c-1-domain.md

---

## 1. DTOs

### Requests

- [ ] `CreateSkuRequest.java` — `record CreateSkuRequest(String barcode, Long sizeId, int implantationQty) {}`
- [ ] `CreateProductRequest.java` — `record CreateProductRequest(String description, BigDecimal price, BigDecimal cost, LocalDate purchaseDate, Long colorId, Long brandId, Long categoryId, Long taxId, List<CreateSkuRequest> skus) {}`
- [ ] `UpdateProductRequest.java` — mesmos campos do create sem a lista de skus
- [ ] `AddSkuRequest.java` — `record AddSkuRequest(String barcode, Long sizeId, int implantationQty) {}`

### Responses

- [ ] `ProductSkuResponse.java` — `record ProductSkuResponse(Long id, String barcode, Long sizeId, String sizeDescription, boolean active)` + `static from(ProductSku, String sizeDescription)`
- [ ] `ProductResponse.java` — produto completo com lista de SKUs enriquecida com nomes de referência; `static from(Product, enriched details...)` — usado apenas nas respostas de escrita (create/update)

---

## 2. Use cases

- [ ] `CreateProductUseCase`
  - Valida que os IDs de color, brand, category, tax existem
  - Chama `Product.create(...)` depois itera `request.skus()` chamando `product.addSku(...)`
  - Salva o aggregate (cascata para SKUs); eventos (`ProductCreated`, `ProductSkuCreated` × N) disparam após o commit

- [ ] `UpdateProductUseCase`
  - Carrega produto → `product.updateDetails(...)` → dirty checking (sem `save()` explícito)
  - Valida IDs referenciados antes de chamar o método de domínio

- [ ] `DeactivateProductUseCase`
  - Carrega produto → `product.deactivate()`

- [ ] `AddProductSkuUseCase`
  - Carrega produto → `product.addSku(barcode, sizeId, implantationQty)` → dirty checking
  - Valida que o `sizeId` existe

- [ ] `DeactivateProductSkuUseCase`
  - Carrega produto → `product.deactivateSku(skuId)`

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

Com `ProductRepository` disponível, adicionar existência guards nos use cases de deleção da phase-02a:

- [ ] `DeleteColorUseCase` — inject `ProductRepository`; throw `IllegalStateException("Color is in use by one or more products")` se `existsByColorId` retornar true
- [ ] `DeleteBrandUseCase` — mesma lógica com `existsByBrandId`
- [ ] `DeleteCategoryUseCase` — mesma lógica com `existsByCategoryId`
- [ ] `DeleteTaxUseCase` — mesma lógica com `existsByTaxId`

---

## 5. Tests

- [ ] `ProductControllerIT`
  - Criar produto com múltiplos SKUs; verificar que `ProductCreated` + `ProductSkuCreated` × N estão registrados no aggregate
  - Atualizar produto; verificar `ProductUpdated` registrado
  - Adicionar SKU a produto existente; verificar `ProductSkuCreated`
  - Desativar SKU; verificar `ProductSkuDeactivated`
  - Desativar produto; verificar `ProductDeactivated`
  - `400` em barcode em branco, `brandId` ausente, `categoryId` ausente
  - `409` em barcode duplicado entre produtos distintos
  - `422` em desativar produto já inativo
- [ ] `DELETE /catalog/colors/{id}` retorna `422` quando um produto referencia aquela cor

---

## 6. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — todos os ITs passam
- [ ] `ModularStructureTest` passa
- [ ] `GET /docs` — endpoints de escrita de produto visíveis no OpenAPI
