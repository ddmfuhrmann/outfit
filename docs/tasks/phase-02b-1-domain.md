# Tasks — Phase 2b-1: Party Domain

Scaffolding do módulo `party`: migração, enums, entidades de domínio e repositórios.
Clientes, fornecedores e vendedores são unificados sob o aggregate root `Party` com flags de papel.

Depends on: none

---

## 1. Flyway migration

- [ ] Create `V7__party_schema.sql`:
  ```sql
  CREATE TABLE party (
      id                 BIGSERIAL PRIMARY KEY,
      person_type        VARCHAR(20)    NOT NULL,
      cnpj               VARCHAR(14),
      cpf                VARCHAR(11),
      legal_name       VARCHAR(200),
      name               VARCHAR(200),
      customer           BOOLEAN        NOT NULL DEFAULT FALSE,
      supplier           BOOLEAN        NOT NULL DEFAULT FALSE,
      salesperson        BOOLEAN        NOT NULL DEFAULT FALSE,
      commission_percent NUMERIC(5,2),
      active             BOOLEAN        NOT NULL DEFAULT TRUE,
      created_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
      updated_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW()
  );

  CREATE TABLE party_address (
      id           BIGSERIAL PRIMARY KEY,
      party_id     BIGINT       NOT NULL REFERENCES party(id),
      street       VARCHAR(300),
      neighborhood VARCHAR(200),
      zip_code     VARCHAR(8),
      number       VARCHAR(20),
      complement   VARCHAR(100),
      city_id      BIGINT       REFERENCES city(id),
      created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
      updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  );

  CREATE TABLE party_contact (
      id             BIGSERIAL PRIMARY KEY,
      party_id       BIGINT      NOT NULL REFERENCES party(id),
      classification VARCHAR(30) NOT NULL,
      description    VARCHAR(200) NOT NULL,
      created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );
  ```

---

## 2. Domain events

Events live in `party/domain/event/`.

- [ ] `PartyCreated.java` — `record PartyCreated(Long partyId, String legalName, boolean customer, boolean supplier, boolean salesperson) {}`
- [ ] `PartyUpdated.java` — `record PartyUpdated(Long partyId) {}`
- [ ] `PartyDeactivated.java` — `record PartyDeactivated(Long partyId) {}`
- [ ] `PartyAddressAdded.java` — `record PartyAddressAdded(Long partyId) {}` — no addressId: child id is null at registerEvent() time (pre-flush); the query module re-indexes the full document from partyId alone
- [ ] `PartyAddressRemoved.java` — `record PartyAddressRemoved(Long partyId, Long addressId) {}` — addressId is available because it is passed in as a method parameter from an already-persisted entity
- [ ] `PartyContactAdded.java` — `record PartyContactAdded(Long partyId) {}` — same rationale as PartyAddressAdded
- [ ] `PartyContactRemoved.java` — `record PartyContactRemoved(Long partyId, Long contactId) {}`

---

## 3. Domain model

### Enums

- [ ] `party/domain/model/PersonType.java` — `LEGAL_ENTITY`, `INDIVIDUAL` (stored as `VARCHAR`)
- [ ] `party/domain/model/ContactType.java` — `PHONE`, `EMAIL`, `WHATSAPP`, `OTHER` (stored as `VARCHAR`)

### Value objects

- [ ] `party/domain/model/Cpf.java` — `@Embeddable`; column `cpf VARCHAR(11)`; `static Cpf.of(String raw)` strips non-digits, validates length == 11, rejects all-same-digit sequences, and verifies both check digits via Módulo 11; `protected Cpf()` for JPA; `String value()` accessor
- [ ] `party/domain/model/Cnpj.java` — `@Embeddable`; column `cnpj VARCHAR(14)`; `static Cnpj.of(String raw)` strips non-digits, validates length == 14, rejects all-same-digit sequences, and verifies both check digits via Módulo 11; `protected Cnpj()` for JPA; `String value()` accessor

### Address (child entity — extends `BaseEntity`)

- [ ] `party/domain/model/Address.java`
  - Fields: `partyId` (Long FK, set by aggregate), `street`, `neighborhood`, `zipCode`, `number`, `complement`, `cityId` (Long — plain FK, not a JPA relationship to avoid cross-module import)
  - `protected Address() {}` for JPA
  - `static Address.create(Long partyId, String street, String neighborhood, String zipCode, String number, String complement, Long cityId)`

### Contact (child entity — extends `BaseEntity`)

- [ ] `party/domain/model/Contact.java`
  - Fields: `partyId` (Long FK), `classification` (ContactType), `description`
  - `protected Contact() {}` for JPA
  - `static Contact.create(Long partyId, ContactType classification, String description)` — guard blank description

### Party (aggregate root — extends `BaseAggregate<Party>`)

- [ ] `party/domain/model/Party.java`
  - Fields: `personType`, `cnpj` (`Cnpj` value object, `@Embedded`), `cpf` (`Cpf` value object, `@Embedded`), `legalName`, `name`, `customer`, `supplier`, `salesperson`, `commissionPercent`, `active`
  - `@OneToMany(mappedBy = "partyId", cascade = CascadeType.ALL, orphanRemoval = true)` for addresses and contacts
  - `protected Party() {}` for JPA
  - `static Party.builder()` — returns a `Party.Builder`; `build()` enforces all invariants (`LEGAL_ENTITY` requires cnpj + legalName; `INDIVIDUAL` requires cpf + legalName; at least one role must be true) and does NOT register `PartyCreated` (getId() is null pre-persist)
  - `@PostPersist void onPersisted()` — registers `PartyCreated(getId(), ...)` after the INSERT; with IDENTITY strategy Hibernate executes the INSERT immediately on persist, so getId() is non-null before Spring Data reads @DomainEvents after save() returns
  - `void updateProfile(String legalName, String name, BigDecimal commissionPercent)` — registers `PartyUpdated`
  - `void deactivate()` — guard already inactive; registers `PartyDeactivated`
  - `Address addAddress(String street, String neighborhood, String zipCode, String number, String complement, Long cityId)` — registers `PartyAddressAdded(getId())`
  - `void removeAddress(Long addressId)` — guard: throw `IllegalStateException` if not found; registers `PartyAddressRemoved(getId(), addressId)`
  - `Contact addContact(ContactType classification, String description)` — registers `PartyContactAdded(getId())`
  - `void removeContact(Long contactId)` — guard: throw `IllegalStateException` if not found; registers `PartyContactRemoved(getId(), contactId)`

---

## 4. Repositories

- [ ] `party/domain/repository/PartyRepository.java`
  - `JpaRepository<Party, Long>` — no custom query methods needed; write-side use cases only use `findById`

---

## 5. Verification

- [ ] `./gradlew assemble` — compiles without errors
- [ ] `ModularStructureTest` passes — `party` does not import from other modules' sub-packages
