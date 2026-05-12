# Tasks — Phase 4b: Sale Query Module

Adds the `sales` Elasticsearch index to the query module. The document is a fully aggregated read model: customer name and seller names come from the existing `parties` index; product descriptions, brand, color, and size come from the existing `products` index. Exposes GET endpoints for list (with search-as-you-type on customer name) and detail.

Depends on: docs/tasks/phase-04b-2-api.md, docs/tasks/phase-04a-3-query-module.md

---

## 1. Index declaration

In `query/infrastructure/config/ElasticsearchIndexInitializer.java` (existing file).

- [ ] Add constant `public static final String INDEX_SALES = "sales";`
- [ ] Add index creation in the `createIndices()` method:
  ```java
  createIndex(client, INDEX_SALES, Map.of(
      "customerName", Map.of("type", "search_as_you_type")
  ));
  ```
  - All other fields use dynamic mapping; only `customerName` needs explicit mapping for search-as-you-type sub-fields

---

## 2. Document records

In `query/application/dto/`.

- [ ] `SaleCustomerDocument.java` — `record SaleCustomerDocument(Long id, String name) {}`
- [ ] `SaleSellerDocument.java` — `record SaleSellerDocument(Long id, String name, BigDecimal sharePercent, BigDecimal commissionPercent) {}`
- [ ] `SaleItemDocument.java` — `record SaleItemDocument(Long skuId, Long productId, String productDescription, String brandDescription, String colorDescription, String sizeDescription, int quantity, BigDecimal unitPrice, BigDecimal itemDiscount, BigDecimal totalPrice) {}`
- [ ] `SaleInstallmentDocument.java` — `record SaleInstallmentDocument(String paymentModality, BigDecimal amount, LocalDate dueDate) {}`
- [ ] `SaleDocument.java`:
  ```java
  record SaleDocument(
      Long saleId,
      String origin,
      Long consignmentId,
      LocalDate saleDate,
      BigDecimal grossAmount,
      BigDecimal storeCreditDiscount,
      BigDecimal netAmount,
      String customerName,
      SaleCustomerDocument customer,
      List<SaleSellerDocument> sellers,
      List<SaleItemDocument> items,
      List<SaleInstallmentDocument> installments,
      Instant createdAt
  ) {}
  ```
  - `customerName` is a top-level field (duplicate of `customer.name`) to enable search-as-you-type mapping

---

## 3. Indexing use case

In `query/application/usecase/`.

Enrichment strategy: all data comes from existing ES indices — `parties` and `products`. No PostgreSQL calls. Use `ElasticsearchIndexInitializer.INDEX_PARTIES` and `INDEX_PRODUCTS` constants.

- [ ] `IndexSaleUseCase.java` — `@Service`
  - `void execute(SaleConfirmed event)`
  - **Customer enrichment** (when `event.customerId()` is non-null): `GET parties/{customerId}` → extract display name (`name` if non-blank, else `legalName`) → build `SaleCustomerDocument`; set top-level `customerName` to same value. When `customerId` is null (walk-in), `customerName = null`, `customer = null`.
  - **Seller enrichment**: for each `SaleSellerSnapshot` in event: `GET parties/{salespersonId}` → extract display name → build `SaleSellerDocument` with `sharePercent` and `commissionPercent` from snapshot
  - **Item enrichment**: for each `SaleItemSnapshot` in event: `GET products/{productId}` → extract `productDescription`, `brandDescription` (from `product.brand.description`), `colorDescription` (from `product.color.description`, nullable); find SKU with matching `skuId` in `product.skus` → extract `sizeDescription`; build `SaleItemDocument`
  - **Installments**: map each `SaleInstallmentSnapshot` → `SaleInstallmentDocument` (no enrichment needed)
  - Index via `client.index(i -> i.index(INDEX_SALES).id(event.saleId().toString()).document(doc))`
  - Wrap infrastructure failures as `IndexingException`

---

## 4. Listener

In `query/application/listener/`.

- [ ] `SaleConfirmedListener.java` — `@Component`
  - `@ApplicationModuleListener void on(SaleConfirmed event)` → `indexSaleUseCase.execute(event)`
  - Note: a `SaleConfirmedListener` also exists in the `inventory` module (stock decrement) — these are independent classes in different packages; Spring Modulith dispatches to both

---

## 5. Read use cases

In `query/application/usecase/`.

- [ ] `GetSaleFromIndexUseCase.java` — `@Service`
  - `SaleDocument execute(Long saleId)`
  - `GET sales/{saleId}` via ES client; wraps as `QueryException` (with 404 cause) if not found
  - Wrap infrastructure failures as `QueryException`

- [ ] `SearchSalesUseCase.java` — `@Service`
  - `PageResponse<SaleDocument> execute(SearchSalesQuery query)`
  - Inner record `SearchSalesQuery(String q, Long customerId, Long salespersonId, String origin, LocalDate from, LocalDate to, int page, int size)`
  - When `q` is non-blank: `multi_match` with `bool_prefix` type on `["customerName", "customerName._2gram", "customerName._3gram", "customerName._index_prefix"]`
  - Term filters (when non-null): `customer.id = customerId`, `sellers.id = salespersonId` (nested term query), `origin`, `saleDate` range `[from, to]`
  - Combine with `bool` query (`must` for text, `filter` for terms/range)
  - Default sort: `saleDate` descending
  - Wrap infrastructure failures as `QueryException`

---

## 6. Controller

In `query/api/rest/`.

- [ ] `SaleQueryController.java` — `@RestController @RequestMapping("/sales")`
  - `GET /sales/{id}` → `GetSaleFromIndexUseCase` → `200 OK`
  - `GET /sales` → `SearchSalesUseCase` (params: `q`, `customerId`, `salespersonId`, `origin`, `from`, `to`, `page`, `size`) → `200 OK`
  - OpenAPI `@Operation` on each method noting: "Served from Elasticsearch. Reflects indexed state after domain events are processed (synchronous)."

---

## 7. Integration tests

In `src/test/java/.../outfit/query/SaleQueryControllerIT.java` — extends `AbstractIT`.

- [ ] `getSaleReturns200WithEnrichedDocument` — create a party (customer), a product with SKU, then POST /sales; `GET /sales/{id}` → response contains `customer.name`, `sellers[].name`, `items[].productDescription`, `items[].brandDescription`, `items[].sizeDescription`
- [ ] `getSaleWithNullCustomerReturns200` — POST /sales without customerId → `GET /sales/{id}` → `customer = null`, `customerName = null`
- [ ] `searchSalesByCustomerNameReturnsMatch` — create sale for named customer, search `?q=<prefix>` → sale appears in results
- [ ] `searchSalesBySellerIdFiltersCorrectly` — create sales for two different sellers; filter `?salespersonId=X` → only sales with seller X returned
- [ ] `searchSalesByOriginFiltersCorrectly` — create DIRECT and CONSIGNMENT sale; filter `?origin=DIRECT` → only direct returned
- [ ] `searchSalesByDateRangeFiltersCorrectly` — create two sales on different dates; filter `?from=&to=` → only matching date returned
- [ ] `getSaleNotFoundReturns404` — unknown ID → 404
- [ ] `consignmentSaleIndexedAfterClose` — issue and close a consignment → `GET /sales/{id}` (using the saleId from the close response) → document exists with `origin = CONSIGNMENT`

---

## 8. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass (Testcontainers Elasticsearch)
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — `GET /sales` and `GET /sales/{id}` visible in OpenAPI with Elasticsearch note
