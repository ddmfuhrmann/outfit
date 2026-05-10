# Tasks — Phase 2c-3: Product Query Module

Elasticsearch product index in the `query` module: event listeners, indexing and bulk-update use cases,
read use cases, and controller. All product reads (search and get by id) are served from Elasticsearch.
The indexer builds documents from **`ProductSnapshot` on domain events** (same idea as `PartyIndexListener` / `IndexPartyUseCase`) — no Postgres reload of aggregate state for indexing.

**Event publication:** indexing runs **synchronously** with the rest of the app: `shared.infrastructure.EventPublicationConfig` registers a `SyncTaskExecutor` as `TaskExecutor`, so `@ApplicationModuleListener` handlers run after the transaction commits and **before** the HTTP response. Product index listeners rely on this (no separate config in `query`). Read-your-writes: a `GET` immediately after a catalog write should see the updated Elasticsearch document. If `EventPublicationConfig` ever switches to async, add an eventual-consistency caveat to these OpenAPI operations.

**Denormalized nested refs:** `ProductDocument` embeds snapshots of color, brand, category, and size (description on each). The `query` module never calls catalog use cases or reads from Postgres — descriptions are resolved exclusively from **ES reference replicas** (`colors`, `brands`, `categories`, `sizes` indices). These replicas are populated by new `*Created` events on the reference aggregates, and kept up to date by `*Renamed` events. When a product event arrives, `IndexProductUseCase` does a GET on the relevant reference index to enrich the document. When a reference is renamed, `ReferenceDataListener` updates the reference replica **and** runs an `UpdateByQuery` on the `products` index so existing documents stay consistent.

Depends on: docs/tasks/phase-02c-2-catalog-api.md  
See event payloads: docs/tasks/phase-02c-1-domain.md

---

## 0. New catalog domain events (prerequisite)

Reference aggregates currently only emit `*Renamed` events. Add creation events so the `query` module can build reference replicas from day one.

- [x] `catalog/domain/event/ColorCreated.java` — `record ColorCreated(Long id, String description) {}`
- [x] `catalog/domain/event/BrandCreated.java` — `record BrandCreated(Long id, String description) {}`
- [x] `catalog/domain/event/CategoryCreated.java` — `record CategoryCreated(Long id, String description) {}`
- [x] `catalog/domain/event/SizeCreated.java` — `record SizeCreated(Long id, String description) {}`

Update each aggregate's static `create()` factory to register the new event (same pattern as other aggregates):
- `Color.create()` → `registerEvent(new ColorCreated(getId(), description))`
- `Brand.create()` → `registerEvent(new BrandCreated(getId(), description))`
- `Category.create()` → `registerEvent(new CategoryCreated(getId(), description))`
- `Size.create()` → `registerEvent(new SizeCreated(getId(), description))`

**`ProductSnapshot` is not changed** — it carries IDs only; descriptions come from the ES reference replicas.

---

## 1. Document records

- [x] `query/application/dto/RefDocument.java`
  ```java
  record RefDocument(Long id, String description) {}
  ```
  Shared by the four reference ES indices (`colors`, `brands`, `categories`, `sizes`).

- [x] `query/application/dto/ProductRefDocument.java`
  ```java
  record ProductRefDocument(Long id, String description) {}
  ```
- [x] `query/application/dto/ProductSkuDocument.java`
  ```java
  record ProductSkuDocument(Long id, String barcode, Long sizeId, String sizeDescription, boolean active) {}
  ```
- [x] `query/application/dto/ProductDocument.java`
  - Fields: `id`, `description`, `price`, `cost`, `purchaseDate`, `active`, `createdAt`, `updatedAt`
  - Nested: `color` (nullable `ProductRefDocument`), `brand`, `category`
  - `List<ProductSkuDocument> skus`

---

## 2. Reference data index use cases

These maintain the ES replicas for the four reference types.

- [x] `query/application/usecase/IndexColorUseCase` — `void execute(Long id, String description)` — upserts `RefDocument` to the `colors` index
- [x] `query/application/usecase/IndexBrandUseCase` — same pattern for `brands`
- [x] `query/application/usecase/IndexCategoryUseCase` — same pattern for `categories`
- [x] `query/application/usecase/IndexSizeUseCase` — same pattern for `sizes`

All: `@Service @Slf4j`. Use `esClient.index(i -> i.index("<name>").id(id.toString()).document(...).refresh(Refresh.True))`. Wrap `IOException` in `IndexingException`.

---

## 3. Event listeners

- [x] `query/application/listener/ProductIndexListener.java`
  - `@ApplicationModuleListener` — not `@Transactional`
  - Handles `ProductCreated`, `ProductUpdated`, `ProductDeactivated`, `ProductSkuCreated`, `ProductSkuDeactivated`
  - Each handler delegates to `IndexProductUseCase.execute(event.snapshot())` — mirror `PartyIndexListener` + `PartySnapshot`

- [x] `query/application/listener/ReferenceDataListener.java`
  - `@ApplicationModuleListener` — not `@Transactional`
  - `ColorCreated` → `indexColor.execute(event.id(), event.description())`
  - `BrandCreated` → `indexBrand.execute(...)`
  - `CategoryCreated` → `indexCategory.execute(...)`
  - `SizeCreated` → `indexSize.execute(...)`
  - `ColorRenamed` → `indexColor.execute(event.colorId(), event.newDescription())` **+** `updateColorInProducts.execute(event.colorId(), event.newDescription())`
  - `BrandRenamed` → `indexBrand.execute(...)` + `updateBrandInProducts.execute(...)`
  - `CategoryRenamed` → `indexCategory.execute(...)` + `updateCategoryInProducts.execute(...)`
  - `SizeRenamed` → `indexSize.execute(...)` + `updateSizeInProducts.execute(...)`

---

## 4. Indexing use case

- [x] `query/application/usecase/IndexProductUseCase`
  - **Signature:** `void execute(ProductSnapshot snapshot)` — imports only `catalog.domain.event.*`
  - Inject `ElasticsearchClient` only — no catalog use cases
  - For each nested ref, GET the description from the corresponding ES replica:
    ```
    color  → GET colors/<colorId>   → ProductRefDocument (null if colorId is null)
    brand  → GET brands/<brandId>   → ProductRefDocument
    category → GET categories/<categoryId> → ProductRefDocument
    per SKU: size → GET sizes/<sizeId> → sizeDescription
    ```
  - Upserts the **full** `ProductDocument` into the `products` index via `ElasticsearchClient` with `Refresh.True` (dynamic mapping — no explicit mapping definition).
  - Product or SKU deactivation is already reflected in the snapshot — **no** partial `UpdateByQuery` / Painless path for SKU-only patches.

---

## 5. Bulk-update use cases (nested reference fields)

These keep **nested objects** inside `ProductDocument` aligned when reference data changes. Required so a rename is visible on `GET /catalog/products` and in search without waiting for a product event.

- [x] `query/application/usecase/UpdateColorNameInProductsUseCase`
  - `UpdateByQuery` on the `products` index: `ctx._source.color.description = params.name` where `color.id == colorId`

- [x] `query/application/usecase/UpdateBrandNameInProductsUseCase`
  - Same pattern: `ctx._source.brand.description = params.name` where `brand.id == brandId`

- [x] `query/application/usecase/UpdateCategoryNameInProductsUseCase`
  - Same pattern: `ctx._source.category.description = params.name` where `category.id == categoryId`

- [x] `query/application/usecase/UpdateSizeNameInProductsUseCase`
  - Iterates nested SKUs: `for (sku in ctx._source.skus) { if (sku.sizeId == params.id) sku.sizeDescription = params.name }`
  - `UpdateByQuery` with a term query on `skus.sizeId`

---

## 6. Read use cases

- [x] `query/application/usecase/SearchProductsUseCase`
  - Accepts `String q`, `Pageable`; runs a multi-match query across all indexed fields
  - Returns `PageResponse<ProductDocument>`

- [x] `query/application/usecase/GetProductByIdUseCase`
  - Fetches the document by `id` in Elasticsearch; throws `ResourceNotFoundException` (mapped to `404`) if missing

---

## 7. Controller

- [x] `query/api/rest/ProductQueryController.java`
  - `GET /catalog/products?q=` → `SearchProductsUseCase`
    - OpenAPI: "Served from Elasticsearch." (With synchronous `EventPublicationConfig`, no eventual-consistency note — see intro.)
  - `GET /catalog/products/{id}` → `GetProductByIdUseCase`
    - OpenAPI: same as search

---

## 8. Tests

- [x] `ProductQueryControllerIT` (Testcontainers Elasticsearch)
  - Setup: create color, brand, category, size via write API (triggers `*Created` events → reference replicas populated)
  - After `ProductCreated`, document is indexed and `GET /catalog/products/{id}` returns it **with color/brand/category/size descriptions**
  - `GET /catalog/products?q=<description>` returns matching documents
  - After deactivation, document shows `active: false`
  - After SKU deactivation, embedded SKU shows `active: false`

- [x] `ReferenceDataListenerTest` (Testcontainers Elasticsearch)
  - `ColorCreated` → `colors` index populated
  - `ColorRenamed` → `colors` index updated **and** `color.description` updated on all product documents referencing that color
  - `BrandRenamed` → same pattern
  - `CategoryRenamed` → same pattern
  - `SizeRenamed` → `sizeDescription` updated on SKUs with that `sizeId`

---

## 9. Verification

- [x] `./gradlew build` — green
- [x] `./gradlew test` — all ITs pass, including Testcontainers ES
- [x] `ModularStructureTest` passes — `query` imports only `catalog.domain.event`; no catalog implementation packages
- [ ] `GET /docs` — read endpoints visible; description states data is served from Elasticsearch (no eventual-consistency caveat while `EventPublicationConfig` uses `SyncTaskExecutor`)
- [ ] `ProductSkuCreated` structure is stable — public contract with `inventory` (phase 3)
