# PRD — Phase 3d: Stock Read Projections

Depends on: docs/prd/phase-03a-initial-stock.md

## Goal

Provide fast, filterable stock read endpoints backed by Elasticsearch. Two projections are maintained: a current stock snapshot (one document per SKU) and a monthly stock history (one document per SKU per calendar month). Both are updated asynchronously after each `StockEntryRecorded` event, making them eventually consistent with the write store.

---

## Scope

**In scope**

- `stock_snapshot` Elasticsearch index — current balance per SKU enriched with product, brand, category, color, and size reference data
- `stock_monthly` Elasticsearch index — monthly inbound/outbound summary and closing balance per SKU
- `GET /inventory/balance/{skuId}` — current snapshot for a single SKU
- `POST /inventory/balance/bulk` — current snapshots for a list of SKUs
- `GET /inventory/stock/monthly` — monthly history with filters

**Out of scope**

- Stock movement history (served from PostgreSQL by phase 3a)
- Analytics aggregations across products (phase 7 query module)
- Synchronous (read-your-writes) consistency — these endpoints are explicitly eventually consistent

---

## Projections

### `stock_snapshot` — one document per SKU

Updated on every `StockEntryRecorded` event. The `currentBalance` field is set to the `runningBalance` carried by the event — no additional database query is needed.

```json
{
  "skuId": 10,
  "barcode": "7891234560001",
  "sizeId": 1,
  "sizeDescription": "P",
  "productId": 5,
  "productDescription": "Camiseta Básica",
  "active": true,
  "brandId": 1,
  "brandDescription": "Nike",
  "categoryId": 2,
  "categoryDescription": "Camisetas",
  "colorId": 3,
  "colorDescription": "Branco",
  "currentBalance": 13,
  "updatedAt": "2026-05-10T09:15:00Z"
}
```

Filterable fields: `productId`, `brandId`, `categoryId`, `colorId`, `active`, `currentBalance`.

### `stock_monthly` — one document per `(skuId, yearMonth)`

Updated on every `StockEntryRecorded` event.

- **First entry of the month for a SKU:** the document is created. `openingBalance = runningBalance − quantity` (derived from the event, no Postgres query). `closingBalance = runningBalance`.
- **Subsequent entries in the same month:** `totalInbound` or `totalOutbound` is incremented according to the sign of `quantity`. `closingBalance` is updated to the latest `runningBalance`.

```json
{
  "skuId": 10,
  "productId": 5,
  "yearMonth": "2026-05",
  "brandId": 1,
  "categoryId": 2,
  "openingBalance": 8,
  "totalInbound": 10,
  "totalOutbound": -5,
  "closingBalance": 13
}
```

Filterable fields: `skuId`, `productId`, `brandId`, `categoryId`, `yearMonth`.

`openingBalance + totalInbound + totalOutbound` always equals `closingBalance`.

---

## Domain events consumed

| Event | From | Action |
|---|---|---|
| `StockEntryRecorded` | `inventory` | Updates `stock_snapshot` and `stock_monthly` for the affected SKU |

Event processing is **asynchronous** — Elasticsearch is updated after the HTTP response returns. All read endpoints must document eventual consistency.

---

## REST API

All endpoints require a valid JWT. All endpoints are served from Elasticsearch.

| Method | Path | Description |
|---|---|---|
| `GET` | `/inventory/balance/{skuId}` | Current stock snapshot for one SKU |
| `POST` | `/inventory/balance/bulk` | Current stock snapshots for a list of SKUs |
| `GET` | `/inventory/stock/monthly` | Monthly stock history with filters |

### Get balance — response

```json
{
  "skuId": 10,
  "productId": 5,
  "productDescription": "Camiseta Básica",
  "barcode": "7891234560001",
  "sizeDescription": "P",
  "brandDescription": "Nike",
  "categoryDescription": "Camisetas",
  "colorDescription": "Branco",
  "currentBalance": 13,
  "updatedAt": "2026-05-10T09:15:00Z"
}
```

Returns `404` if no snapshot exists for the SKU (no stock entry has ever been recorded).

### Bulk balance — request body

```json
{ "skuIds": [10, 11, 12] }
```

Returns a list (not paginated) of snapshot documents for the requested SKUs. SKUs with no snapshot are omitted from the response.

### Monthly history — query parameters

| Parameter | Type | Description |
|---|---|---|
| `skuId` | `Long` | Optional — filter by SKU |
| `productId` | `Long` | Optional — filter by product |
| `brandId` | `Long` | Optional — filter by brand |
| `categoryId` | `Long` | Optional — filter by category |
| `yearMonth` | `String` | Optional — exact match, format `YYYY-MM` |
| `page` | `int` | Default 0 |
| `size` | `int` | Default 20 |

Returns `PageResponse<StockMonthlyDocument>`.

---

## Acceptance criteria

- [ ] After `ProductSkuCreated` with `implantationQty = 10` is processed — `GET /inventory/balance/{skuId}` returns `currentBalance = 10`.
- [ ] After a manual adjustment that sets balance to 25 — `GET /inventory/balance/{skuId}` returns `currentBalance = 25` (eventually).
- [ ] `POST /inventory/balance/bulk` with three SKU ids — returns documents for all three; a SKU with no snapshot is omitted.
- [ ] `GET /inventory/balance/{skuId}` for an unknown SKU — returns `404`.
- [ ] `GET /inventory/stock/monthly?skuId=10&yearMonth=2026-05` — returns the correct monthly document with consistent `openingBalance + totalInbound + totalOutbound = closingBalance`.
- [ ] Two movements in the same month for a SKU — the monthly document accumulates both; `closingBalance` reflects the final `runningBalance`.
- [ ] First movement of a new month — `openingBalance` equals the `runningBalance` of the last movement of the previous month.
- [ ] `GET /inventory/stock/monthly?brandId=1` — returns only documents for SKUs belonging to brand 1.
- [ ] All three read endpoints document eventual consistency in OpenAPI.
- [ ] `ModularStructureTest` passes.
- [ ] `./gradlew test` green (Testcontainers Elasticsearch).
