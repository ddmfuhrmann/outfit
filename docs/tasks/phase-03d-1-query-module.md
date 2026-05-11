# Tasks — Phase 3d-1: Stock Read Projections

Elasticsearch projections for current stock (`stock_snapshot`) and monthly history (`stock_monthly`).
Listener consumes `StockEntryRecorded` from the `inventory` module and updates both indices asynchronously.

Depends on: docs/tasks/phase-03a-1-domain.md

---

## 0. Per-listener async executor

The global `EventPublicationConfig` stays unchanged (`SyncTaskExecutor`) so catalog listeners
remain synchronous. Only the stock projection listener runs asynchronously, using a dedicated
executor bean scoped to the `query` module.

- [ ] `query/infrastructure/elasticsearch/StockProjectionExecutorConfig.java` — `@Configuration`
  ```java
  @Bean("stockProjectionExecutor")
  public TaskExecutor stockProjectionExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(2);
      executor.setMaxPoolSize(10);
      executor.setQueueCapacity(500);
      executor.setThreadNamePrefix("stock-proj-");
      executor.initialize();
      return executor;
  }
  ```

---

## 1. Index constants and initializer

- [ ] Extend `query/infrastructure/elasticsearch/ElasticsearchIndexInitializer.java` with new index names and field name constants:
  ```java
  public static final String STOCK_SNAPSHOT_INDEX = "stock_snapshot";
  public static final String STOCK_MONTHLY_INDEX  = "stock_monthly";

  // stock_snapshot field names
  public static final String FIELD_SKU_ID            = "skuId";
  public static final String FIELD_CURRENT_BALANCE   = "currentBalance";
  public static final String FIELD_UPDATED_AT        = "updatedAt";

  // stock_monthly field names
  public static final String FIELD_YEAR_MONTH        = "yearMonth";
  public static final String FIELD_OPENING_BALANCE   = "openingBalance";
  public static final String FIELD_TOTAL_INBOUND     = "totalInbound";
  public static final String FIELD_TOTAL_OUTBOUND    = "totalOutbound";
  public static final String FIELD_CLOSING_BALANCE   = "closingBalance";
  ```
- [ ] In the initializer's `@PostConstruct` method, create both indices if they do not exist:
  - `stock_snapshot`: explicit mapping for `skuId` (`keyword`), `productId` (`keyword`), `brandId` (`keyword`), `categoryId` (`keyword`), `colorId` (`keyword`), `active` (`boolean`), `currentBalance` (`integer`), `updatedAt` (`date`)
  - `stock_monthly`: explicit mapping for `skuId` (`keyword`), `productId` (`keyword`), `brandId` (`keyword`), `categoryId` (`keyword`), `yearMonth` (`keyword`), `openingBalance` (`integer`), `totalInbound` (`integer`), `totalOutbound` (`integer`), `closingBalance` (`integer`)
  - Wrap `IOException` in `IndexingException`

---

## 2. Document records

- [ ] `query/application/dto/StockSnapshotDocument.java`
  ```java
  record StockSnapshotDocument(
      Long skuId, String barcode, Long sizeId, String sizeDescription,
      Long productId, String productDescription, boolean active,
      Long brandId, String brandDescription,
      Long categoryId, String categoryDescription,
      Long colorId, String colorDescription,
      int currentBalance, Instant updatedAt) {}
  ```

- [ ] `query/application/dto/StockMonthlyDocument.java`
  ```java
  record StockMonthlyDocument(
      Long skuId, Long productId,
      Long brandId, Long categoryId,
      String yearMonth,
      int openingBalance, int totalInbound, int totalOutbound, int closingBalance) {}
  ```

---

## 3. Indexing use cases

- [ ] `query/application/usecase/UpdateStockSnapshotUseCase.java` — `@Service`
  - Accepts `StockEntryRecorded event`
  - Fetches the existing `StockSnapshotDocument` from `stock_snapshot` by `skuId`
  - If document exists: partial update via `esClient.update()` setting `currentBalance = event.runningBalance()` and `updatedAt = event.occurredAt()`
  - If document does not exist (first entry for this SKU): build a full `StockSnapshotDocument` by fetching reference data from existing ES indices:
    - SKU/product/brand/category/color/size descriptions from the `products` index (`GET products/<productId>`) — the product document already has all enriched fields
    - Upsert via `esClient.index(...)`
  - Wrap `IOException` in `IndexingException`

- [ ] `query/application/usecase/UpdateStockMonthlyUseCase.java` — `@Service`
  - Accepts `StockEntryRecorded event`
  - Document id: `"{skuId}-{yearMonth}"` where `yearMonth = YearMonth.from(event.occurredAt().atZone(ZoneOffset.UTC))` formatted as `"YYYY-MM"`
  - Fetch existing document from `stock_monthly` by this id
  - If document does not exist (first entry for this SKU in this month):
    - `openingBalance = event.runningBalance() - event.quantity()` (balance before this entry, derived from event — no Postgres query)
    - `totalInbound` or `totalOutbound` from `event.quantity()`
    - `closingBalance = event.runningBalance()`
    - Create document via `esClient.index(...)`
  - If document exists: script update via `esClient.update()` with inline Painless:
    ```
    if (params.qty > 0) { ctx._source.totalInbound += params.qty }
    else { ctx._source.totalOutbound += params.qty }
    ctx._source.closingBalance = params.closingBalance
    ```
    Params: `qty = event.quantity()`, `closingBalance = event.runningBalance()`
  - Wrap `IOException` in `IndexingException`

---

## 4. Listener

- [ ] `query/application/listener/StockEntryRecordedListener.java`
  - Annotate the handler method with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` and `@Async("stockProjectionExecutor")` — **not** `@ApplicationModuleListener`, because that annotation always uses the global `taskExecutor` and cannot be overridden per-listener
  - Handles `inventory.domain.event.StockEntryRecorded`
  - Delegates to `UpdateStockSnapshotUseCase.execute(event)` then `UpdateStockMonthlyUseCase.execute(event)`
  - Not `@Transactional`

---

## 5. Read use cases

- [ ] `query/application/usecase/GetStockSnapshotUseCase.java` — `@Service`
  - Fetches `StockSnapshotDocument` from `stock_snapshot` by `skuId`; throws `ResourceNotFoundException` (→ `404`) if absent

- [ ] `query/application/usecase/GetStockSnapshotBulkUseCase.java` — `@Service`
  - Accepts `List<Long> skuIds`
  - Uses `esClient.mget(...)` to fetch all documents in one round-trip
  - Returns only the found documents (missing SKUs are silently omitted)

- [ ] `query/application/usecase/SearchStockSnapshotUseCase.java` — `@Service`
  - Accepts `Long productId` (optional), `Long brandId` (optional), `Long categoryId` (optional), `Pageable`
  - Builds a bool query with `term` filters for each non-null parameter
  - Returns `PageResponse<StockSnapshotDocument>`

- [ ] `query/application/usecase/GetStockMonthlyUseCase.java` — `@Service`
  - Accepts `Long skuId` (optional), `Long productId` (optional), `Long brandId` (optional), `Long categoryId` (optional), `String yearMonth` (optional), `Pageable`
  - Builds a bool query with `term` filters for each non-null parameter
  - Returns `PageResponse<StockMonthlyDocument>`

---

## 6. DTOs for read endpoints

- [ ] `query/application/dto/BulkBalanceRequest.java`
  ```java
  record BulkBalanceRequest(List<Long> skuIds) {}
  ```

---

## 7. Controller

- [ ] `query/api/rest/StockQueryController.java`
  - `GET /inventory/balance/{skuId}` → `GetStockSnapshotUseCase`; returns `StockSnapshotDocument`
    - OpenAPI: "Served from Elasticsearch. Eventually consistent."
  - `GET /inventory/balance?productId=&brandId=&categoryId=&page=&size=` → `SearchStockSnapshotUseCase`; returns `PageResponse<StockSnapshotDocument>`
    - All query params optional; no params returns all snapshots paginated
    - OpenAPI: "Served from Elasticsearch. Eventually consistent."
  - `POST /inventory/balance/bulk` → `GetStockSnapshotBulkUseCase`; returns `List<StockSnapshotDocument>`
    - OpenAPI: "Served from Elasticsearch. Eventually consistent."
  - `GET /inventory/stock/monthly?skuId=&productId=&brandId=&categoryId=&yearMonth=&page=&size=` → `GetStockMonthlyUseCase`; returns `PageResponse<StockMonthlyDocument>`
    - OpenAPI: "Served from Elasticsearch. Eventually consistent."

---

## 8. Tests

- [ ] `StockQueryControllerIT` (Testcontainers Elasticsearch + PostgreSQL)
  - Setup: create product + SKU with `implantationQty = 10`; wait for async listener to complete before asserting
    - Use `Awaitility` with a short timeout (≤ 3 s) to poll until the ES document appears
  - `GET /inventory/balance/{skuId}` → `currentBalance = 10`; all reference fields populated from the `products` index
  - `GET /inventory/balance/{skuId}` for unknown SKU → `404`
  - After a manual adjustment to 25 → `GET /inventory/balance/{skuId}` (after Awaitility) returns `currentBalance = 25`
  - `GET /inventory/balance?productId={id}` → returns all SKU snapshots for that product
  - `GET /inventory/balance?brandId={id}` → returns only snapshots for SKUs belonging to that brand
  - `GET /inventory/balance?categoryId={id}` → returns only snapshots for SKUs in that category
  - `GET /inventory/balance` with no params → returns all snapshots paginated
  - `POST /inventory/balance/bulk` with two SKU ids → returns both documents; an unknown SKU is omitted
  - `GET /inventory/stock/monthly?skuId={id}&yearMonth={currentMonth}` → document with correct `openingBalance`, `totalInbound`, `closingBalance`
  - Two movements in the same month → monthly document accumulates both; `closingBalance` matches final `runningBalance`
  - `GET /inventory/stock/monthly?brandId={id}` → returns only documents for SKUs belonging to that brand

---

## 9. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass including Testcontainers Elasticsearch
- [ ] `ModularStructureTest` passes — `query` listener imports only `inventory.domain.event.StockEntryRecorded`
- [ ] `GET /docs` — all four stock read endpoints visible; descriptions state Elasticsearch and eventual consistency
