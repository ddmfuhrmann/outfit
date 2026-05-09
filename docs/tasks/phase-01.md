# Tasks — Phase 1: Foundation

Reference PRD: [`docs/prd/phase-01-foundation.md`](../prd/phase-01-foundation.md)

---

## 1. Infrastructure

- [ ] Create `docker-compose.yml` with `db` (postgres:16) and `es` (elasticsearch:8.13.4) services
- [ ] Populate `src/main/resources/application.yaml` with datasource, JPA, Flyway, and JWT config
- [ ] Add SpringDoc OpenAPI dependency to `build.gradle.kts` and configure `/docs` endpoint
- [ ] Add JJWT dependencies to `build.gradle.kts` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson` 0.12.6)

## 2. Module structure test

- [ ] Create `ModularStructureTest` at `src/test/java/github/io/ddmfuhrmann/outfit/ModularStructureTest.java`
- [ ] Verify `./gradlew test` passes with the empty module structure

## 3. Shared kernel

- [ ] Create `shared/domain/model/BaseEntity.java` — `@MappedSuperclass` with `id`, `createdAt`, `updatedAt`
- [ ] Enable JPA auditing (`@EnableJpaAuditing`) on main application class or a `@Configuration`
- [ ] Create `shared/application/dto/PageResponse.java` — generic record with static `from(Page<T>)` factory
- [ ] Create `shared/api/rest/GlobalExceptionHandler.java` — `@RestControllerAdvice` with mappings for 400/403/404/409/500

## 4. Flyway migrations

- [ ] Create `src/main/resources/db/migration/V1__shared_schema.sql` — `city`, `company`, `app_user` tables
- [ ] Create `src/main/resources/db/migration/V2__seed_admin_user.sql` — insert default `admin` user (BCrypt hash)

## 5. Shared domain model

- [ ] Create `shared/domain/model/City.java` — extends `BaseEntity`, fields per PRD
- [ ] Create `shared/domain/model/Company.java` — extends `BaseEntity`, `@ManyToOne` to `City`
- [ ] Create `shared/domain/model/UserRole.java` — enum `ADMIN`, `USER`
- [ ] Create `shared/domain/model/User.java` — extends `BaseEntity`, fields per PRD, `role` stored as `VARCHAR`
- [ ] Create `shared/domain/repository/CityRepository.java` — `JpaRepository<City, Long>` + search by name
- [ ] Create `shared/domain/repository/CompanyRepository.java` — `JpaRepository<Company, Long>`
- [ ] Create `shared/domain/repository/UserRepository.java` — `JpaRepository<User, Long>` + `findByLogin`

## 6. Auth (JWT)

- [ ] Create `shared/infrastructure/security/JwtProperties.java` — `@ConfigurationProperties("outfit.security.jwt")`
- [ ] Create `shared/infrastructure/security/JwtService.java` — `generateToken(User)`, `extractLogin(token)`, `isValid(token)`
- [ ] Create `shared/infrastructure/security/JwtAuthFilter.java` — `OncePerRequestFilter` reading `Authorization` header
- [ ] Create `shared/infrastructure/security/SecurityConfig.java` — `SecurityFilterChain` bean: stateless, CSRF off, permit `/auth/**`, add `JwtAuthFilter`
- [ ] Create `shared/infrastructure/security/UserDetailsServiceImpl.java` — loads `User` by login, maps `UserRole` to `GrantedAuthority`

## 7. Auth API

- [ ] Create `shared/application/dto/LoginRequest.java` — record `{login, password}`
- [ ] Create `shared/application/dto/LoginResponse.java` — record `{token, expiresAt}`
- [ ] Create `shared/application/usecase/LoginUseCase.java` — authenticates credentials, returns `LoginResponse`
- [ ] Create `shared/api/rest/AuthController.java` — `POST /auth/login` → delegates to `LoginUseCase`

## 8. Shared CRUD API

- [ ] Create `shared/application/dto/CityResponse.java`
- [ ] Create `shared/application/usecase/ListCitiesUseCase.java`
- [ ] Create `shared/application/usecase/GetCityUseCase.java`
- [ ] Create `shared/api/rest/CityController.java` — `GET /shared/cities`, `GET /shared/cities/{id}`
- [ ] Create `shared/application/dto/CompanyResponse.java`, `UpdateCompanyRequest.java`
- [ ] Create `shared/application/usecase/GetCompanyUseCase.java`, `UpdateCompanyUseCase.java`
- [ ] Create `shared/api/rest/CompanyController.java` — `GET /shared/company`, `PUT /shared/company`
- [ ] Create `shared/application/dto/UserResponse.java` (no `passwordHash`), `CreateUserRequest.java`, `UpdateUserRequest.java`
- [ ] Create `shared/application/usecase/ListUsersUseCase.java`, `CreateUserUseCase.java`, `GetUserUseCase.java`, `UpdateUserUseCase.java`, `DeactivateUserUseCase.java`
- [ ] Create `shared/api/rest/UserController.java` — full CRUD per PRD (all `ADMIN`-only)

## 9. Tests

- [ ] Integration test: `AuthControllerIT` — login success, login failure, protected endpoint with/without token
- [ ] Integration test: `CityControllerIT` — list and get with valid token
- [ ] Integration test: `UserControllerIT` — CRUD with ADMIN token, 403 with USER token
- [ ] Verify `ModularStructureTest` still passes after all code is in place

## 10. Verification checklist

Run through every acceptance criterion in the PRD before closing this phase:

- [ ] `docker-compose up` — no errors
- [ ] `./gradlew bootRun` — starts cleanly against containers
- [ ] Flyway migrations apply on fresh DB
- [ ] All acceptance criteria in PRD checked off
- [ ] `./gradlew test` — green, no skipped tests
- [ ] `GET /docs` — OpenAPI spec loads
