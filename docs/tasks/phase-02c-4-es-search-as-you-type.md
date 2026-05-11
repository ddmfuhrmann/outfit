# Tasks — Phase 2c-4: Elasticsearch Search-As-You-Type Index Mappings

Introduces explicit index mappings to the `query` module so that all six Elasticsearch indices support
prefix/typeahead search out of the box. Key text fields are declared as `search_as_you_type`;
all other fields remain dynamic. Search use cases are updated to query the generated sub-fields
using `bool_prefix`, enabling single-character prefix matches across products, parties, and reference data.

Depends on: docs/tasks/phase-02c-3-query-module.md

---

## 1. Index initializer

- [ ] `query/infrastructure/config/ElasticsearchIndexInitializer.java` — `@Component @Slf4j`
  - `@EventListener(ApplicationReadyEvent.class)` method `initIndices()` iterates all six indices
  - For each index: check existence with `esClient.indices().exists(r -> r.index(name))` — create only if absent
  - Wrap `IOException` in `RuntimeException` to fail-fast on misconfiguration at startup; do not swallow

**Mapping per index (Java builder DSL):**

`products`:
```java
esClient.indices().create(c -> c
    .index("products")
    .mappings(m -> m
        .dynamic(DynamicMapping.True)
        .properties("description", p -> p.searchAsYouType(s -> s))
    )
);
```

`parties`:
```java
esClient.indices().create(c -> c
    .index("parties")
    .mappings(m -> m
        .dynamic(DynamicMapping.True)
        .properties("name",      p -> p.searchAsYouType(s -> s))
        .properties("legalName", p -> p.searchAsYouType(s -> s))
    )
);
```

`brands`, `colors`, `categories`, `sizes` — same pattern with a single `description` field:
```java
esClient.indices().create(c -> c
    .index("<name>")
    .mappings(m -> m
        .dynamic(DynamicMapping.True)
        .properties("description", p -> p.searchAsYouType(s -> s))
    )
);
```

Log `INFO "Created ES index: {}"` when an index is created; nothing when it already exists.

---

## 2. Search use cases — query update

Replace plain `multiMatch` with `multi_match / bool_prefix` targeting the sub-fields that
`search_as_you_type` generates automatically (`._2gram`, `._3gram`, `._index_prefix`).

- [ ] `query/application/usecase/SearchProductsUseCase.java`
  ```java
  Query.of(q -> q.multiMatch(mm -> mm
      .query(text)
      .type(TextQueryType.BoolPrefix)
      .fields("description", "description._2gram", "description._3gram", "description._index_prefix")
  ))
  ```

- [ ] `query/application/usecase/SearchBrandsUseCase.java` — same, field `description`
- [ ] `query/application/usecase/SearchColorsUseCase.java` — same, field `description`
- [ ] `query/application/usecase/SearchCategoriesUseCase.java` — same, field `description`
- [ ] `query/application/usecase/SearchSizesUseCase.java` — same, field `description`

- [ ] `query/application/usecase/SearchPartiesUseCase.java`
  - Preserve the existing boolean filter clause (role flags)
  - Replace the text-match sub-query with `bool_prefix` on:
    `["name", "name._2gram", "name._3gram", "name._index_prefix", "legalName", "legalName._2gram", "legalName._3gram", "legalName._index_prefix", "cnpj", "cpf"]`
  - `cnpj` and `cpf` are not `search_as_you_type` — include them as plain fields so exact numeric entry still matches

---

## 3. Tests

- [ ] `ElasticsearchIndexInitializerIT` (Testcontainers ES)
  - Assert that all six indices are created on startup by verifying they exist via `esClient.indices().exists(...)`
  - Assert the `description` field on `products` has type `search_as_you_type` via `esClient.indices().getMapping(...)`
  - Assert `name` and `legalName` on `parties` have type `search_as_you_type`

- [ ] `ProductQueryControllerIT` — add prefix assertion:
  - Index a product with `description = "Camiseta Azul"`; `GET /catalog/products?q=Cam` must return it

- [ ] `PartyQueryControllerIT` — add prefix assertion:
  - Index a party with `name = "João da Silva"`; `GET /party?q=Joã` must return it

- [ ] `SearchBrandsUseCaseIT` — prefix match:
  - Index brand `"Adidas"`; search `q=Adi` returns it

---

## 4. Dev/ops note (non-deliverable — include as comment in initializer)

If the persistent Docker ES already contains indices created with pure dynamic mapping, those indices
must be deleted before the first run with the new initializer — a field type change from `text`
to `search_as_you_type` is not accepted by Elasticsearch on an existing index. In Testcontainers
environments this is a non-issue since ES starts fresh each test run.

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass, including Testcontainers ES
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — no new endpoints; existing endpoints unchanged
- [ ] `GET /catalog/products?q=<prefix>` returns documents matching that prefix
