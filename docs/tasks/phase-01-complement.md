# Tasks — Phase 1 Complement

Adjustments required to bring the `shared` module in line with the DDD and Lombok rules defined in `spec/architecture.md` and `CLAUDE.md` after phase-01 implementation.

All tasks below are pure refactoring — no behavior change, no new endpoints, no schema migration.

---

## C1 — Add Lombok to build.gradle.kts

`@Getter` is needed on entities. Lombok is not yet a dependency.

- [ ] Add to `build.gradle.kts`:
  ```kotlin
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  ```

---

## C2 — Introduce `BaseAggregate<T>`

`AbstractAggregateRoot<T>` from Spring Data enables `registerEvent()` but does not carry audit fields. A typed base class is needed so aggregate roots get both capabilities without duplicating fields.

- [ ] Create `shared/domain/model/BaseAggregate.java`:
  ```java
  @MappedSuperclass
  @EntityListeners(AuditingEntityListener.class)
  public abstract class BaseAggregate<T extends BaseAggregate<T>> extends AbstractAggregateRoot<T> {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @CreatedDate
      @Column(updatable = false)
      private Instant createdAt;

      @LastModifiedDate
      private Instant updatedAt;

      public Long getId() { return id; }
      public Instant getCreatedAt() { return createdAt; }
      public Instant getUpdatedAt() { return updatedAt; }
  }
  ```

`BaseEntity` stays as-is — plain entities that are not aggregate roots continue to extend it.

---

## C3 — Refactor `City` to DDD

`City` is reference data: no domain behavior, no events. Only needs factory + explicit JPA constructor + `@Getter`.

- [ ] Replace manual getters with `@Getter` on the class
- [ ] Add `protected City() {}` no-arg constructor (explicit, for JPA clarity)
- [ ] Add `static City.of(...)` factory:
  ```java
  public static City of(Integer ibgeCityCode, Integer ibgeStateCode,
                        String cityName, String stateName, String stateAbbr) {
      var city = new City();
      city.ibgeCityCode = ibgeCityCode;
      city.ibgeStateCode = ibgeStateCode;
      city.cityName = cityName;
      city.stateName = stateName;
      city.stateAbbr = stateAbbr;
      return city;
  }
  ```

---

## C4 — Refactor `User` to DDD aggregate root

`User` currently has public setters and is instantiated with `new` from use cases. It must become a proper aggregate root with factory and domain methods.

- [ ] Change `extends BaseEntity` to `extends BaseAggregate<User>`
- [ ] Add `@Getter` on the class; remove all manual getters and **all setters**
- [ ] Add `protected User() {}` no-arg constructor
- [ ] Add `static User.create(...)` factory:
  ```java
  public static User create(String login, String passwordHash, String name, UserRole role) {
      var user = new User();
      user.login = login;
      user.passwordHash = passwordHash;
      user.name = name;
      user.role = role;
      user.active = true;
      user.registerEvent(new UserCreated(user.login, user.name, user.role));
      return user;
  }
  ```
- [ ] Add domain behavior methods:
  ```java
  public void updateProfile(String name) {
      this.name = name;
  }

  public void changeRole(UserRole role) {
      this.role = role;
  }

  public void deactivate() {
      this.active = false;
      registerEvent(new UserDeactivated(this.login));
  }
  ```

---

## C5 — Create domain events for `User`

`registerEvent()` calls in C4 depend on these records existing first.

- [ ] Create `shared/domain/event/UserCreated.java`:
  ```java
  public record UserCreated(String login, String name, UserRole role) {}
  ```
- [ ] Create `shared/domain/event/UserDeactivated.java`:
  ```java
  public record UserDeactivated(String login) {}
  ```

---

## C6 — Refactor `Company` to domain model

`Company` is a singleton seeded by Flyway — no factory needed, no events. It stays on `BaseEntity` and gains a domain method in place of its setters.

- [ ] Add `@Getter` on the class; remove all manual getters and **all setters**
- [ ] Add `protected Company() {}` no-arg constructor
- [ ] Add domain method `update(...)`:
  ```java
  public void update(String cnpj, String companyName, String tradeName,
                     String street, String phone, City city) {
      this.cnpj = cnpj;
      this.companyName = companyName;
      this.tradeName = tradeName;
      this.street = street;
      this.phone = phone;
      this.city = city;
  }
  ```

---

## C7 — Refactor `User` use cases

- [ ] **`CreateUserUseCase`** — replace `new User()` + setters with factory:
  ```java
  var user = User.create(request.login(), passwordEncoder.encode(request.password()),
                         request.name(), request.role());
  return UserResponse.from(userRepository.save(user));
  ```

- [ ] **`UpdateUserUseCase`** — replace setters with domain methods; remove explicit `save()` (dirty checking handles it within `@Transactional`):
  ```java
  var user = userRepository.findById(id).orElseThrow(...);
  if (request.name() != null) user.updateProfile(request.name());
  if (request.role() != null) user.changeRole(request.role());
  return UserResponse.from(user);
  ```

- [ ] **`DeactivateUserUseCase`** — replace `user.setActive(false)` with domain method; remove explicit `save()`:
  ```java
  var user = userRepository.findById(id).orElseThrow(...);
  user.deactivate();
  ```

---

## C8 — Refactor `UpdateCompanyUseCase`

- [ ] Replace all setters with the `update(...)` domain method; remove explicit `save()`:
  ```java
  var company = companyRepository.findById(1L).orElseThrow(...);
  City city = request.cityId() != null
      ? cityRepository.findById(request.cityId()).orElseThrow(...)
      : null;
  company.update(request.cnpj(), request.companyName(), request.tradeName(),
                 request.street(), request.phone(), city);
  return CompanyResponse.from(company);
  ```

---

## C9 — Remove `active` from `UpdateUserRequest`

`active` state is managed exclusively via `DeactivateUserUseCase`. Having it in the update request allows bypassing the domain method and its event.

- [ ] Remove the `active` field from `UpdateUserRequest`
- [ ] Remove the `if (request.active() != null) user.setActive(request.active())` branch from `UpdateUserUseCase` (already gone after C7, but confirm the DTO is clean)

---

## C10 — Add domain invariants to entities

Entities must enforce their own rules regardless of the caller. DTO validation (`@NotBlank`) protects only the HTTP boundary; the domain layer must be self-protecting.

Throw `IllegalArgumentException` for invalid input state and `IllegalStateException` for invalid operation sequences. `GlobalExceptionHandler` must be updated to map both to `400 Bad Request`.

- [ ] **`GlobalExceptionHandler`** — add handlers for domain violations:
  ```java
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String, Object>> handleDomainArgument(IllegalArgumentException ex, HttpServletRequest req) {
      return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<Map<String, Object>> handleDomainState(IllegalStateException ex, HttpServletRequest req) {
      return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req.getRequestURI());
  }
  ```

- [ ] **`User.create()`** — guard required fields:
  ```java
  if (login == null || login.isBlank()) throw new IllegalArgumentException("login is required");
  if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("passwordHash is required");
  if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
  if (role == null) throw new IllegalArgumentException("role is required");
  ```

- [ ] **`User.updateProfile()`** — guard name:
  ```java
  if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
  ```

- [ ] **`User.changeRole()`** — guard role:
  ```java
  if (role == null) throw new IllegalArgumentException("role is required");
  ```

- [ ] **`User.deactivate()`** — guard already-inactive:
  ```java
  if (!this.active) throw new IllegalStateException("User is already inactive");
  ```

- [ ] **`Company.update()`** — guard required fields:
  ```java
  if (cnpj == null || cnpj.isBlank()) throw new IllegalArgumentException("cnpj is required");
  if (!cnpj.matches("\\d{14}")) throw new IllegalArgumentException("cnpj must be 14 digits");
  if (companyName == null || companyName.isBlank()) throw new IllegalArgumentException("companyName is required");
  ```

- [ ] **`City.of()`** — guard required fields:
  ```java
  if (ibgeCityCode == null) throw new IllegalArgumentException("ibgeCityCode is required");
  if (cityName == null || cityName.isBlank()) throw new IllegalArgumentException("cityName is required");
  if (stateAbbr == null || stateAbbr.length() != 2) throw new IllegalArgumentException("stateAbbr must be 2 characters");
  ```

---

## C11 — Verify

- [ ] `./gradlew build` — green, no compilation errors from removed setters
- [ ] `./gradlew test` — all ITs still pass (behavior unchanged)
- [ ] `ModularStructureTest` passes
- [ ] IT coverage for new invariants: test that `deactivate()` on an already-inactive user returns `422`, and that required-field violations return `400`
