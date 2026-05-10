# PRD — Phase 2c: Product Catalog

## Goal

Deliver the product catalog with a grid model: `Product` is the header aggregate (color, brand, category, tax,
price, cost) and `ProductSku` is the child entity representing the actual sellable variant (size + barcode).
All downstream modules (inventory, sales, purchasing) reference `ProductSku` — never `Product` directly.

---

## Scope

**In scope**

- `Product` aggregate root + `ProductSku` child entity (Flyway migration V8)
- Full CRUD for products including SKU management
- `ProductSkuCreated` domain event carrying `implantationQty` — the public contract with the `inventory` module
- Existence guards on reference data deletes (color, brand, category, tax cannot be deleted while in use)
- Elasticsearch product index in the `query` module — all fields, dynamic mapping
- `GET /catalog/products` (search) and `GET /catalog/products/{id}` served from Elasticsearch

**Out of scope**

- Inventory stock entries (phase 3 consumes `ProductSkuCreated`)
- Price history or variant-level pricing

---

## Domain model

### Product (aggregate root)

| Field | Type | Notes |
|---|---|---|
| `description` | `String` | Required |
| `price` | `BigDecimal` | Required, `NUMERIC(15,2)` |
| `cost` | `BigDecimal` | Required, `NUMERIC(15,2)` |
| `purchaseDate` | `LocalDate` | Nullable |
| `colorId` | `Long` | FK to `color.id`, nullable |
| `brandId` | `Long` | FK to `brand.id`, required |
| `categoryId` | `Long` | FK to `category.id`, required |
| `taxId` | `Long` | FK to `tax.id`, required |
| `active` | `boolean` | Default true |

All monetary values are `BigDecimal` — no `float` or `double`.

### ProductSku (child entity)

| Field | Type | Notes |
|---|---|---|
| `barcode` | `String` | Required, unique across the entire table |
| `sizeId` | `Long` | FK to `size.id`, required |
| `active` | `boolean` | Default true |

`implantationQty` is **not** a field on `ProductSku`. It is carried only in the request DTO and forwarded
via the `ProductSkuCreated` domain event to the `inventory` module.

### Domain events

| Event | Payload | Published when |
|---|---|---|
| `ProductCreated` | `productId`, `description` | After `Product.create(...)` |
| `ProductUpdated` | `productId` | After `product.updateDetails(...)` |
| `ProductDeactivated` | `productId` | After `product.deactivate()` |
| `ProductSkuCreated` | `skuId`, `productId`, `sizeId`, `barcode`, `implantationQty` | After each `product.addSku(...)` |
| `ProductSkuDeactivated` | `skuId`, `productId` | After `product.deactivateSku(skuId)` |

`ProductSkuCreated` is the **public API contract** between `catalog` and `inventory`. Its record shape
must not change without coordinating the inventory listener in phase 3.

The `query` module listens to all five events above to keep the Elasticsearch index in sync.

Additionally, the `query` module listens to `ColorRenamed`, `BrandRenamed`, `CategoryRenamed`, and `SizeRenamed`
(published by the `catalog` module in phase 2a) and issues `UpdateByQuery` operations to propagate the new name
into all denormalized product documents — without reloading from PostgreSQL.

### Invariants

- `description`, `price`, `cost`, `brandId`, `categoryId`, `taxId` are required on `Product.create(...)`.
- `barcode` must be unique within the aggregate (enforced in the domain method) and at the database level (unique constraint).
- `price` and `cost` must be non-negative.
- Deactivating an already-inactive product or SKU throws `IllegalStateException`.

---

## REST API

All endpoints require a valid JWT.

### Write endpoints (`catalog` module)

| Method | Path | Description |
|---|---|---|
| `POST` | `/catalog/products` | Create product with initial SKUs |
| `PUT` | `/catalog/products/{id}` | Update product header fields |
| `DELETE` | `/catalog/products/{id}` | Deactivate product |
| `POST` | `/catalog/products/{id}/skus` | Add SKU to existing product |
| `DELETE` | `/catalog/products/{id}/skus/{skuId}` | Deactivate SKU |

### Read endpoints (`query` module — served from Elasticsearch)

| Method | Path | Description |
|---|---|---|
| `GET` | `/catalog/products` | Full-text search. Query param: `?q=` (searches all indexed fields). Returns `PageResponse<ProductDocument>`. |
| `GET` | `/catalog/products/{id}` | Fetch product document by id from Elasticsearch. |

Read endpoints are eventually consistent: they reflect the state of the Elasticsearch index, which is updated asynchronously after each domain event. OpenAPI must document this.

### Create product — request body

```json
{
  "description": "Camiseta Básica",
  "price": 59.90,
  "cost": 25.00,
  "purchaseDate": "2026-05-01",
  "colorId": 3,
  "brandId": 1,
  "categoryId": 2,
  "taxId": 1,
  "skus": [
    { "barcode": "7891234560001", "sizeId": 1, "implantationQty": 10 },
    { "barcode": "7891234560002", "sizeId": 2, "implantationQty": 15 }
  ]
}
```

`implantationQty` appears in the request but is absent from all response bodies.

---

## Elasticsearch index

Index name: `products`

Dynamic mapping is enabled — no explicit mapping definition required. The document shape mirrors the full product with embedded SKUs, enriched with reference data names (color, brand, category, tax, size descriptions) so that no join is needed at query time.

```json
{
  "id": 1,
  "description": "Camiseta Básica",
  "price": 59.90,
  "cost": 25.00,
  "purchaseDate": "2026-05-01",
  "active": true,
  "color":    { "id": 3, "description": "Branco" },
  "brand":    { "id": 1, "description": "Nike" },
  "category": { "id": 2, "description": "Camisetas" },
  "tax":      { "id": 1, "description": "Simples Nacional" },
  "skus": [
    { "id": 10, "barcode": "7891234560001", "sizeId": 1, "sizeDescription": "P", "active": true },
    { "id": 11, "barcode": "7891234560002", "sizeId": 2, "sizeDescription": "M", "active": true }
  ],
  "createdAt": "2026-05-09T12:00:00Z",
  "updatedAt": "2026-05-09T12:00:00Z"
}
```

The `query` module builds and updates this document by listening to `ProductCreated`, `ProductUpdated`, `ProductDeactivated`, `ProductSkuCreated`, and `ProductSkuDeactivated`.

---

## Reference data delete guards

Once the product catalog exists, deleting a reference entity that is in use must be blocked:

| Endpoint | Guard |
|---|---|
| `DELETE /catalog/colors/{id}` | `422` if any product references that color |
| `DELETE /catalog/brands/{id}` | `422` if any product references that brand |
| `DELETE /catalog/categories/{id}` | `422` if any product references that category |
| `DELETE /catalog/taxes/{id}` | `422` if any product references that tax |

Size delete is not exposed (sizes are deactivated, not deleted, together with their SKUs).

---

## Acceptance criteria

- [ ] Create a product with two SKUs — both appear in `GET /catalog/products/{id}` (Elasticsearch).
- [ ] `ProductCreated` and two `ProductSkuCreated` events (each carrying correct `implantationQty`) are registered on the aggregate after creation.
- [ ] After creation, `GET /catalog/products?q=<description>` returns the product from Elasticsearch.
- [ ] `POST` with a duplicate barcode within the same product returns `400`.
- [ ] `POST` with a duplicate barcode already existing in another product returns `409` (database unique constraint).
- [ ] `POST` with missing `brandId`, `categoryId`, or `taxId` returns `400`.
- [ ] Add a SKU to an existing product — `ProductSkuCreated` event registered, SKU visible in subsequent `GET /catalog/products/{id}`.
- [ ] Deactivate a SKU — `ProductSkuDeactivated` event registered, document in Elasticsearch shows SKU `active: false`.
- [ ] Deactivate an already-inactive product returns `422`.
- [ ] `DELETE /catalog/colors/{id}` on a color used by a product returns `422`.
- [ ] `ModularStructureTest` passes (no cross-module sub-package imports).
- [ ] All endpoints visible in OpenAPI at `GET /docs`; read endpoints note eventual consistency.
- [ ] `./gradlew test` green.
