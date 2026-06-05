---
date: 2026-06-04
title: phase-04-remaining-sale-storecredit-commission — Query Analysis (v2 — post-optimization)
mode: verification (changes were applied before this run)
---

# Query Analysis Report — v2 (Post-Optimization Verification)

## Summary

All 6 findings from the v1 report have been resolved. The `Persistable<Long>` fix to `BaseEntity`/`BaseAggregate` eliminated every SELECT-before-INSERT across the codebase. The `CreateProductUseCase` consolidation reduced reference lookups from 6 SELECTs (3 `existsById` + 3 `findById`) to 3 (one `findById` each). The salesperson double-lookup was eliminated by passing `List<SalespersonDetails>` directly into `CreateCommissionsFromSaleUseCase`. The V15 Flyway migration added all 5 missing indexes. The extra `UPDATE sale` after INSERT is no longer observed. The login now issues exactly 1 SELECT on `app_user`.

## Method

- App started with `--spring.profiles.active=sqllog` (temporary `application-sqllog.yaml` created and deleted post-run)
- Hibernate SQL logging at `DEBUG`, bind parameters at `TRACE`, Hibernate statistics enabled
- Log collected at `/tmp/outfit-sqllog-v2.txt` (334 lines at startup + full request lifecycle)
- Endpoints exercised: `POST /auth/login`, `POST /catalog/brands`, `POST /catalog/products` (1 SKU), `POST /party`, `POST /sales` (1 item, 1 seller, 1 installment)
- Database schema verified via `docker exec outfit-postgres psql -U outfit -d outfit -c "\d <table>"`
- Source code reviewed: `BaseEntity`, `BaseAggregate`, `CreateProductUseCase`, `CreateSaleUseCase`, `CreateCommissionsFromSaleUseCase`, `LoginUseCase`, `UserPrincipal`, `V15__commission_storecredit_indexes.sql`

---

## Verification Results

| Finding | Severity | Status | Before | After |
|---|---|---|---|---|
| SELECT-before-INSERT (Persistable) | HIGH | RESOLVED | 1 SELECT per entity before every INSERT | 0 — direct INSERT only |
| Double reference lookup (CreateProductUseCase) | MEDIUM | RESOLVED | 6 SELECTs (existsById×3 + findById×3) | 3 SELECTs (findById×3, each once) |
| Double salesperson lookup (POST /sales) | MEDIUM | RESOLVED | 2 × SELECT party per seller | 1 × SELECT party per seller |
| Missing indexes (seller_commission, store_credit_note) | MEDIUM | RESOLVED | No indexes on filter columns | 5 indexes present in DB |
| Extra UPDATE sale after INSERT | LOW | RESOLVED | UPDATE sale SET version=1 after INSERT | No UPDATE sale — absent from trace |
| Double user SELECT on login | LOW | RESOLVED | 2 × SELECT app_user on login | 1 × SELECT app_user on login |

---

## Detailed Findings

### Finding 1 — SELECT-before-INSERT [was HIGH]

**Status: RESOLVED**

`BaseEntity` and `BaseAggregate` now implement `Persistable<Long>` with `isNew() { return createdAt == null; }`, annotated `@Transient`. Because `createdAt` is `null` before the `@CreatedDate` auditing listener fires on first persist, Hibernate calls `persist()` (not `merge()`) and issues a direct INSERT with no existence-check SELECT.

**Evidence — POST /catalog/brands:**
```sql
-- v1 (before): SELECT then INSERT
select b1_0.id, b1_0.created_at, b1_0.description, b1_0.updated_at from brand b1_0 where b1_0.id=?
insert into brand (created_at, description, updated_at, id) values (?, ?, ?, ?)

-- v2 (after): INSERT only
insert into brand (created_at, description, updated_at, id) values (?, ?, ?, ?)
```

**Evidence — POST /party:**
```sql
-- v2 (after): direct INSERT on party — no prior SELECT
insert into party (active, cnpj, commission_percent, cpf, created_at, customer,
    legal_name, name, person_type, salesperson, supplier, updated_at, id)
    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

Session metrics confirm `0 nanoseconds spent preparing 0 JDBC statements` in the listener session (no extra round-trips after the main INSERT session).

---

### Finding 2 — Double reference lookup in CreateProductUseCase [was MEDIUM]

**Status: RESOLVED**

`CreateProductUseCase` now calls `findById().orElseThrow()` once per reference entity (brand, category, size) and reuses the returned object for both validation and response DTO construction. The previous `existsById` pass was removed entirely.

**Evidence — POST /catalog/products (1 SKU): 3 SELECTs for references (down from 6)**
```sql
-- 1. Brand lookup (once)
select b1_0.id, b1_0.created_at, b1_0.description, b1_0.updated_at from brand b1_0 where b1_0.id=?

-- 2. Category lookup (once)
select c1_0.id, c1_0.created_at, c1_0.description, c1_0.ncm_code, c1_0.updated_at
    from category c1_0 where c1_0.id=?

-- 3. Size lookup (batch IN clause, all SKU sizeIds in one query)
select s1_0.id, s1_0.created_at, s1_0.description, s1_0.updated_at
    from size s1_0 where s1_0.id in (?)

-- Then: INSERT product, INSERT product_sku — no additional reference SELECTs
insert into product (...) values (...)
insert into product_sku (...) values (...)
```

Total SQL statements for POST /catalog/products (1 SKU): **13** (3 SELECTs for references + 1 product INSERT + 1 SKU INSERT + 3 event_publication INSERTs + 1 stock_balance SELECT + 1 stock_entry INSERT + 1 event_publication INSERT + 1 stock_balance UPDATE + 2 event_publication UPDATEs). Previously was 16+ due to duplicate reference SELECTs.

---

### Finding 3 — Double salesperson lookup in POST /sales [was MEDIUM]

**Status: RESOLVED**

`CreateSaleUseCase.fetchSellerDetails()` now calls `GetSalespersonDetailsService.execute()` once per seller and passes the resulting `List<SalespersonDetails>` into `CreateCommissionsFromSaleUseCase.execute(sale, sellerDetails)`. The commission use case builds a `Map<Long, SalespersonDetails>` from that list instead of querying the DB again.

**Evidence — POST /sales (1 seller): 1 SELECT on party (down from 2)**
```sql
-- Ordered SQL trace (request lines 1892–2182):
-- #1: SELECT party — exactly once for GetSalespersonDetailsService
select p1_0.id, p1_0.active, p1_0.cnpj, p1_0.commission_percent, p1_0.cpf,
    p1_0.created_at, p1_0.customer, p1_0.legal_name, p1_0.name,
    p1_0.person_type, p1_0.salesperson, p1_0.supplier, p1_0.updated_at
    from party p1_0 where p1_0.id=?

-- #2: SELECT commission_bonus_tier (correct — needed for bonus calculation)
-- #3–#9: INSERTs (sale, sale_installment, sale_item, event_publication×2,
--         seller_commission, sale_seller)
-- #10: SELECT stock_balance (for inventory deduction)
-- #11–#12: INSERT stock_entry, event_publication
-- #13–#15: UPDATE stock_balance, event_publication×2
```

Total SQL for POST /sales (1 item, 1 seller, 1 installment): **15 statements** (3 SELECTs + 9 INSERTs + 3 UPDATEs). Previously was ~21 due to the extra party SELECT and the SELECT-before-INSERT on every entity.

---

### Finding 4 — Missing indexes on seller_commission and store_credit_note [was MEDIUM]

**Status: RESOLVED**

V15 migration (`V15__commission_storecredit_indexes.sql`) was applied. Verified via `\d` in psql.

**Evidence — seller_commission indexes:**
```
"idx_seller_commission_salesperson" btree (salesperson_id)
"idx_seller_commission_sale_date" btree (sale_date DESC)
"idx_seller_commission_status" btree (status)
```

**Evidence — store_credit_note indexes:**
```
"idx_store_credit_note_customer" btree (customer_id)
"idx_store_credit_note_status" btree (status)
```

All 5 indexes are present in the live database (Flyway reported "Successfully validated 15 migrations" on startup).

---

### Finding 5 — Extra UPDATE sale after INSERT [was LOW]

**Status: RESOLVED**

No `UPDATE sale` statement appears in the POST /sales trace. The full list of UPDATE statements in the request is:

```sql
-- Only legitimate UPDATEs:
update stock_balance set current_balance=?, updated_at=?, version=? where product_sku_id=? and version=?
update event_publication djep1_0 set completion_date=? where djep1_0.id=?
update event_publication djep1_0 set completion_date=? where djep1_0.id=?
```

The `sale` table is written exactly once (INSERT). The root cause — `CreateCommissionsFromSaleUseCase` triggering a Hibernate dirty-check by re-reading `sale.getSellers()` within the same transaction — is gone because the use case now receives seller data as a parameter (`List<SalespersonDetails>`) and does not access the `Sale` aggregate at all after `saleRepository.save()`.

---

### Finding 6 — Double user SELECT on POST /auth/login [was LOW]

**Status: RESOLVED**

`LoginUseCase` now casts the `Authentication` principal to `UserPrincipal` and extracts `principal.user()` directly, instead of calling `userRepository.findById()` for a second lookup. `UserPrincipal` is a record wrapping the `User` entity loaded by `UserDetailsServiceImpl.loadUserByUsername()` during authentication.

**Evidence — POST /auth/login: 1 SELECT on app_user (down from 2)**
```sql
-- Only one SELECT, in the authentication phase:
select u1_0.login, u1_0.active, u1_0.created_at, u1_0.name,
    u1_0.password_hash, u1_0.role, u1_0.updated_at
    from app_user u1_0 where u1_0.login=?
-- Session metrics: 1 JDBC statement prepared, 1 executed — no second lookup
```

---

## New Findings

No new SQL issues were discovered during this run.

**Ordering note (informational, not a bug):** In the POST /sales trace, `INSERT INTO seller_commission` occurs before `INSERT INTO sale_seller`. This is because `CreateCommissionsFromSaleUseCase` is called before the `Sale` aggregate's `@ElementCollection` is flushed by JPA dirty-checking. There is no foreign key from `seller_commission` to `sale_seller`, so the ordering is safe. The `sale_seller` FK is only to `sale(id)`, which is already persisted at that point.

---

## Elasticsearch Query Analysis

Unchanged from v1. All query-module `Search*UseCase` classes use `filter` context for scalar filters, `multi_match` with `BoolPrefix` for text search, and no unbounded full-table scans. Minor observations from v1 remain valid as low-priority improvements:

1. `SearchProductsUseCase`: top-level `multiMatch` cannot be combined with future `filter` clauses without a `bool` refactor.
2. `matchAll` queries on empty parameters do not specify a `sort` — pagination order may be unstable across pages.

Neither issue has changed or worsened since v1.

---

## Recommended Next Steps

All previously identified findings are resolved. The following are the only remaining low-priority items from the Elasticsearch analysis:

1. **[LOW] `SearchProductsUseCase`** — wrap the top-level `multiMatch` in a `bool { must }` to allow future `filter` clauses (e.g., `active=true`) without rewriting the query builder.

2. **[LOW] Stable ES pagination** — add `sort: [{ "_id": "asc" }]` to all `matchAll` query paths to guarantee deterministic page ordering when no relevance score is present.

3. **[INFORMATIONAL] JWT filter SELECT per authenticated request** — `JwtAuthFilter` issues 1 `SELECT app_user` per request (for token validation via `loadUserByUsername`). This was not flagged in v1 as a duplicate-lookup issue (only the login double-lookup was), and remains the expected behavior. At high request rates, adding Spring Security's `UserCache` or extracting role/name directly from JWT claims would eliminate this recurring DB hit.
