# Tasks — Phase 2c-3: Product Query Module

Índice Elasticsearch de produtos no módulo `query`: listeners de eventos, use cases de indexação e bulk-update,
use cases de leitura e controller. Toda leitura de produto (search e get by id) é servida pelo Elasticsearch.

Depends on: docs/tasks/phase-02c-2-catalog-api.md

---

## 1. Document records

- [ ] `query/application/dto/ProductRefDocument.java`
  ```java
  record ProductRefDocument(Long id, String description) {}
  ```
- [ ] `query/application/dto/ProductSkuDocument.java`
  ```java
  record ProductSkuDocument(Long id, String barcode, Long sizeId, String sizeDescription, boolean active) {}
  ```
- [ ] `query/application/dto/ProductDocument.java`
  - Fields: `id`, `description`, `price`, `cost`, `purchaseDate`, `active`, `createdAt`, `updatedAt`
  - Nested: `color` (nullable `ProductRefDocument`), `brand`, `category`, `tax`
  - `List<ProductSkuDocument> skus`

---

## 2. Read-only query repository

- [ ] `query/infrastructure/persistence/ProductQueryRepository.java`
  - Acessa as tabelas `product`, `product_sku`, `color`, `brand`, `category`, `tax`, `size` diretamente via `JdbcTemplate` (sem importar nenhuma classe de `catalog.*`)
  - Método principal: `ProductDocument findFullById(Long productId)` — retorna o documento enriquecido com todos os nomes de referência necessários para indexação

---

## 3. Event listeners

- [ ] `query/application/listener/ProductIndexListener.java`
  - `@ApplicationModuleListener` — not `@Transactional`
  - Handles `ProductCreated`, `ProductUpdated`, `ProductDeactivated`, `ProductSkuCreated`, `ProductSkuDeactivated`
  - Delegates to `IndexProductUseCase` passing `productId`

- [ ] `query/application/listener/ReferenceDataRenameListener.java`
  - `@ApplicationModuleListener` — not `@Transactional`
  - Handles `ColorRenamed`, `BrandRenamed`, `CategoryRenamed`, `SizeRenamed`
  - Delegates to the corresponding bulk-update use case

---

## 4. Indexing use case

- [ ] `query/application/usecase/IndexProductUseCase`
  - Carrega documento completo via `ProductQueryRepository.findFullById(productId)`
  - Faz upsert no índice `products` via `ElasticsearchClient` (dynamic mapping — sem definição de mapping explícita)
  - Em `ProductDeactivated`: re-indexa o documento com `active: false`
  - Em `ProductSkuDeactivated`: atualiza apenas o SKU afetado no documento via `UpdateByQuery` com script Painless

---

## 5. Bulk-update use cases (fan-out on rename)

- [ ] `query/application/usecase/UpdateColorNameInProductsUseCase`
  - `UpdateByQuery` no índice `products`: `ctx._source.color.description = params.name` where `color.id == colorId`

- [ ] `query/application/usecase/UpdateBrandNameInProductsUseCase`
  - Mesmo padrão: `ctx._source.brand.description = params.name` where `brand.id == brandId`

- [ ] `query/application/usecase/UpdateCategoryNameInProductsUseCase`
  - Mesmo padrão: `ctx._source.category.description = params.name` where `category.id == categoryId`

- [ ] `query/application/usecase/UpdateSizeNameInProductsUseCase`
  - Itera sobre SKUs aninhados: `for (sku in ctx._source.skus) { if (sku.sizeId == params.id) sku.sizeDescription = params.name }`
  - `UpdateByQuery` com nested query em `skus.sizeId`

---

## 6. Read use cases

- [ ] `query/application/usecase/SearchProductsUseCase`
  - Aceita `String q`, `Pageable`; executa multi-match query em todos os campos indexados
  - Retorna `PageResponse<ProductDocument>`

- [ ] `query/application/usecase/GetProductByIdUseCase`
  - Busca documento por `id` no Elasticsearch; lança `ResourceNotFoundException` (mapeado para `404`) se ausente

---

## 7. Controller

- [ ] `query/api/rest/ProductQueryController.java`
  - `GET /catalog/products?q=` → `SearchProductsUseCase`
    - OpenAPI: "Served from Elasticsearch. Eventually consistent — reflects the state of the index after domain events are processed."
  - `GET /catalog/products/{id}` → `GetProductByIdUseCase`
    - OpenAPI: mesma nota de eventual consistency

---

## 8. Tests

- [ ] `ProductQueryControllerIT` (Testcontainers Elasticsearch)
  - Após evento `ProductCreated`, documento indexado e `GET /catalog/products/{id}` retorna
  - `GET /catalog/products?q=<description>` retorna documentos correspondentes
  - Após desativação, documento mostra `active: false`
  - Após desativação de SKU, SKU embutido mostra `active: false`

- [ ] `ReferenceDataRenameListenerTest` (Testcontainers Elasticsearch)
  - `ColorRenamed` → `color.description` atualizado em todos os documentos que referenciam aquela cor
  - `BrandRenamed` → `brand.description` atualizado
  - `CategoryRenamed` → `category.description` atualizado
  - `SizeRenamed` → `sizeDescription` atualizado nos SKUs com aquele `sizeId`

---

## 9. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — todos os ITs passam incluindo Testcontainers ES
- [ ] `ModularStructureTest` passa — `query` não importa sub-pacotes de `catalog`
- [ ] `GET /docs` — endpoints de leitura visíveis com nota de eventual consistency
- [ ] Estrutura de `ProductSkuCreated` estável — contrato público com `inventory` (phase 3)
