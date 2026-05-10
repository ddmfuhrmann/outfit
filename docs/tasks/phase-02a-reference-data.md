# Tasks — Phase 2a: Reference Data

Reference data entities needed by the catalog module: sizes, colors, brands, and categories.
All are aggregate roots with full CRUD endpoints.

---

## 1. Flyway migration

- [ ] Create `V6__catalog_reference_schema.sql`:
  ```sql
  CREATE TABLE size (
      id          BIGSERIAL PRIMARY KEY,
      description VARCHAR(100) NOT NULL,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );

  CREATE TABLE color (
      id          BIGSERIAL PRIMARY KEY,
      description VARCHAR(100) NOT NULL,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );

  CREATE TABLE brand (
      id          BIGSERIAL PRIMARY KEY,
      description VARCHAR(100) NOT NULL,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );

  CREATE TABLE category (
      id          BIGSERIAL PRIMARY KEY,
      description VARCHAR(100) NOT NULL,
      ncm_code    VARCHAR(10),
      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );

  ```

---

## 2. Domain events

Events live in `catalog/domain/event/`. They are published on rename so the `query` module can fan-out `UpdateByQuery` to denormalized Elasticsearch documents.

- [ ] `SizeRenamed.java` — `record SizeRenamed(Long sizeId, String newDescription) {}`
- [ ] `ColorRenamed.java` — `record ColorRenamed(Long colorId, String newDescription) {}`
- [ ] `BrandRenamed.java` — `record BrandRenamed(Long brandId, String newDescription) {}`
- [ ] `CategoryRenamed.java` — `record CategoryRenamed(Long categoryId, String newDescription) {}`

---

## 3. Domain model

Each extends `BaseAggregate<T>`. All use `@Getter` only. No public setters. Factory method + domain methods only.

### Size

- [ ] `catalog/domain/model/Size.java`
  - `static Size.create(String description)` — guard blank description
  - `void rename(String description)` — guard blank; registers `SizeRenamed`

### Color

- [ ] `catalog/domain/model/Color.java`
  - `static Color.create(String description)`
  - `void rename(String description)` — registers `ColorRenamed`

### Brand

- [ ] `catalog/domain/model/Brand.java`
  - `static Brand.create(String description)`
  - `void rename(String description)` — registers `BrandRenamed`

### Category

- [ ] `catalog/domain/model/Category.java`
  - Fields: `description`, `ncmCode`
  - `static Category.create(String description, String ncmCode)`
  - `void rename(String description, String ncmCode)` — registers `CategoryRenamed`

---

## 4. Repositories

- [ ] `catalog/domain/repository/SizeRepository.java` — `JpaRepository<Size, Long>` + `existsByDescription`
- [ ] `catalog/domain/repository/ColorRepository.java` — same pattern
- [ ] `catalog/domain/repository/BrandRepository.java`
- [ ] `catalog/domain/repository/CategoryRepository.java`
---

## 5. DTOs

Records only. Response DTOs have a `static from(Entity)` factory. Request DTOs carry raw fields.

- [ ] `SizeRequest.java` — `record SizeRequest(String description) {}`
- [ ] `SizeResponse.java` — `record SizeResponse(Long id, String description, Instant createdAt)` + `from(Size)`
- [ ] `ColorRequest.java`, `ColorResponse.java`
- [ ] `BrandRequest.java`, `BrandResponse.java`
- [ ] `CategoryRequest.java` — `record CategoryRequest(String description, String ncmCode) {}`
- [ ] `CategoryResponse.java` — includes `ncmCode`
---

## 6. Use cases

One class per operation. `@Transactional` on write operations only.

### Size
- [ ] `CreateSizeUseCase` — `Size.create(...)` → save → return `SizeResponse.from(...)`
- [ ] `GetSizeUseCase` — find by id or throw `ResourceNotFoundException`
- [ ] `ListSizesUseCase` — `Pageable` → `PageResponse<SizeResponse>`
- [ ] `RenameSizeUseCase` — load → `size.rename(...)` → dirty checking (no explicit save)

### Color, Brand, Category
- [ ] Same four use cases for each entity (`Create`, `Get`, `List`, `Rename`)
- [ ] `DeleteSizeUseCase` — delete; integrity enforced by PostgreSQL FK (no guard needed now)
- [ ] `DeleteColorUseCase` — same pattern
- [ ] `DeleteBrandUseCase`
- [ ] `DeleteCategoryUseCase`

---

## 7. Controllers

All routes are secured (no permit-all needed beyond existing `SecurityConfig`).

- [ ] `catalog/api/rest/SizeController.java`
  - `POST /catalog/sizes` → `CreateSizeUseCase`
  - `GET /catalog/sizes` → `ListSizesUseCase`
  - `GET /catalog/sizes/{id}` → `GetSizeUseCase`
  - `PUT /catalog/sizes/{id}` → `RenameSizeUseCase`
  - `DELETE /catalog/sizes/{id}` → `DeleteSizeUseCase` (204; PostgreSQL FK enforces integrity)

- [ ] `catalog/api/rest/ColorController.java` — same pattern at `/catalog/colors`
- [ ] `catalog/api/rest/BrandController.java` — `/catalog/brands`
- [ ] `catalog/api/rest/CategoryController.java` — `/catalog/categories`

---

## 8. Tests

- [ ] `SizeControllerIT` — create, list, get, rename, delete; assert `200`, `201`, `204`, `404`, `400` on blank description
- [ ] `ColorControllerIT`, `BrandControllerIT`, `CategoryControllerIT` — same coverage including DELETE
- [ ] Assert rename fires domain event: check `SizeRenamed` (and equivalents) is registered via `AbstractAggregateRoot` event list in a unit test on the domain class

---

## 9. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes — no cross-module leakage
- [ ] `GET /docs` — all new endpoints visible in OpenAPI
