# Tasks — Phase 4a: Consignment Query Module

Adds the `consignments` Elasticsearch index to the query module. The document is created on `ConsignmentIssued` and updated on `ConsignmentItemsReturned` and `ConsignmentClosed`. Enrichment uses the existing `parties` and `products` indices — no PostgreSQL calls. Exposes GET endpoints for list (with search-as-you-type on customer name) and detail.

Depends on: docs/tasks/phase-04a-2-api.md

---

## 1. Index declaration

In `query/infrastructure/config/ElasticsearchIndexInitializer.java` (existing file).

- [ ] Add constant `public static final String INDEX_CONSIGNMENTS = "consignments";`
- [ ] Add index creation in the `createIndices()` method:
  ```java
  createIndex(client, INDEX_CONSIGNMENTS, Map.of(
      "customerName", Map.of("type", "search_as_you_type")
  ));
  ```
  - All other fields use dynamic mapping; only `customerName` needs explicit mapping for search-as-you-type sub-fields

---

## 2. Document records

In `query/application/dto/`.

- [ ] `ConsignmentCustomerDocument.java` — `record ConsignmentCustomerDocument(Long id, String name) {}`
- [ ] `ConsignmentSellerDocument.java` — `record ConsignmentSellerDocument(Long id, String name) {}`
- [ ] `ConsignmentItemDocument.java` — `record ConsignmentItemDocument(Long skuId, Long productId, String productDescription, String brandDescription, String colorDescription, String sizeDescription, int quantityIssued, int quantityReturned, int quantitySold, BigDecimal unitPrice) {}`
- [ ] `ConsignmentDocument.java`:
  ```java
  record ConsignmentDocument(
      Long consignmentId,
      String status,
      LocalDate issueDate,
      Instant closedAt,
      String customerName,
      ConsignmentCustomerDocument customer,
      List<ConsignmentSellerDocument> sellers,
      List<ConsignmentItemDocument> items,
      Instant updatedAt
  ) {}
  ```

---

## 3. Indexing use cases

In `query/application/usecase/`.

Enrichment strategy: all data comes from existing ES indices — `parties` (for customer and seller names) and `products` (for product/brand/color/size descriptions). No PostgreSQL calls. Use `ElasticsearchIndexInitializer.INDEX_PARTIES` and `INDEX_PRODUCTS` constants.

- [ ] `IndexConsignmentUseCase.java` — `@Service`
  - `void execute(ConsignmentIssued event)`
  - For `customerId`: `GET parties/{customerId}` → extract display name (`name` if non-blank, else `legalName`)
  - For each `salespersonId`: `GET parties/{id}` → extract display name
  - For each item's `productId`: `GET products/{productId}` → extract `productDescription`, `brandDescription`, `colorDescription`; find SKU with matching `skuId` in the product's `skus` list → extract `sizeDescription`
  - Build `ConsignmentDocument` with `status = "OPEN"`, `quantityReturned = 0`, `quantitySold = 0` for all items
  - Index via `client.index(i -> i.index(INDEX_CONSIGNMENTS).id(event.consignmentId().toString()).document(doc))`
  - Wrap infrastructure failures as `IndexingException`

- [ ] `UpdateConsignmentReturnUseCase.java` — `@Service`
  - `void execute(ConsignmentItemsReturned event)`
  - `GET consignments/{consignmentId}` → load existing document
  - For each returned item in the event: find matching `ConsignmentItemDocument` by `skuId`; add `event.quantity()` to `quantityReturned`; recompute `quantitySold = quantityIssued - quantityReturned`
  - Re-index updated document with `updatedAt = Instant.now()`
  - Wrap infrastructure failures as `IndexingException`

- [ ] `UpdateConsignmentStatusUseCase.java` — `@Service`
  - `void execute(ConsignmentClosed event)`
  - `GET consignments/{consignmentId}` → load existing document
  - Update `status = "CLOSED"`, `closedAt = event.closedAt()`, `updatedAt = Instant.now()`; update each item's `quantitySold` from event `soldItems`
  - Re-index updated document
  - Wrap infrastructure failures as `IndexingException`

---

## 4. Listeners

In `query/application/listener/`.

- [ ] `ConsignmentIssuedListener.java` — `@Component`
  - `@ApplicationModuleListener void on(ConsignmentIssued event)` → `indexConsignmentUseCase.execute(event)`

- [ ] `ConsignmentItemsReturnedListener.java` — `@Component`
  - `@ApplicationModuleListener void on(ConsignmentItemsReturned event)` → `updateConsignmentReturnUseCase.execute(event)`

- [ ] `ConsignmentClosedListener.java` — `@Component`
  - `@ApplicationModuleListener void on(ConsignmentClosed event)` → `updateConsignmentStatusUseCase.execute(event)`

---

## 5. Read use cases

In `query/application/usecase/`.

- [ ] `GetConsignmentFromIndexUseCase.java` — `@Service`
  - `ConsignmentDocument execute(Long consignmentId)`
  - `GET consignments/{consignmentId}` via ES client; throws `QueryException` wrapping `EntityNotFoundException` if not found
  - Wrap infrastructure failures as `QueryException`

- [ ] `SearchConsignmentsUseCase.java` — `@Service`
  - `PageResponse<ConsignmentDocument> execute(SearchConsignmentsQuery query)`
  - Inner record `SearchConsignmentsQuery(String q, Long customerId, Long salespersonId, String status, LocalDate from, LocalDate to, int page, int size)`
  - When `q` is non-blank: `multi_match` with `bool_prefix` type on `["customerName", "customerName._2gram", "customerName._3gram", "customerName._index_prefix"]`
  - Term filters (when non-null): `customer.id = customerId`, `sellers.id = salespersonId`, `status`, `issueDate` range `[from, to]`
  - Combine with `bool` query (`must` for text, `filter` for terms/range)
  - Wrap infrastructure failures as `QueryException`

---

## 6. Controller

In `query/api/rest/`.

- [ ] `ConsignmentQueryController.java` — `@RestController @RequestMapping("/consignments")`
  - `GET /consignments/{id}` → `GetConsignmentFromIndexUseCase` → `200 OK`
  - `GET /consignments` → `SearchConsignmentsUseCase` (params: `q`, `customerId`, `salespersonId`, `status`, `from`, `to`, `page`, `size`) → `200 OK`
  - OpenAPI `@Operation` on each method noting: "Served from Elasticsearch. Reflects indexed state after domain events are processed (synchronous)."

---

## 7. Integration tests

In `src/test/java/.../outfit/query/ConsignmentQueryControllerIT.java` — extends `AbstractIT`.

- [ ] `getConsignmentReturns200WithEnrichedDocument` — issue a consignment, then `GET /consignments/{id}` → response contains `customer.name`, `sellers[].name`, `items[].productDescription`
- [ ] `getConsignmentAfterReturnReflectsUpdatedQuantities` — issue, return 1 unit → `GET /consignments/{id}` → `items[].quantityReturned = 1`, `quantitySold` updated
- [ ] `getConsignmentAfterCloseReflectsStatusClosed` — close consignment → `GET /consignments/{id}` → `status = CLOSED`, `closedAt` non-null
- [ ] `searchConsignmentsByCustomerNameReturnsMatch` — issue consignment for a customer, search `?q=<prefix of customer name>` → consignment appears in results
- [ ] `searchConsignmentsByStatusFiltersCorrectly` — create OPEN and CLOSED; filter `?status=OPEN` → only open returned
- [ ] `getConsignmentNotFoundReturns404` — unknown ID → 404

---

## 8. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass (Testcontainers Elasticsearch)
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — `GET /consignments` and `GET /consignments/{id}` visible in OpenAPI with Elasticsearch note
