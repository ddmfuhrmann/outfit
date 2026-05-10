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
      company_name       VARCHAR(200),
      trade_name         VARCHAR(200),
      is_customer        BOOLEAN        NOT NULL DEFAULT FALSE,
      is_supplier        BOOLEAN        NOT NULL DEFAULT FALSE,
      is_salesperson     BOOLEAN        NOT NULL DEFAULT FALSE,
      commission_percent NUMERIC(5,2),
      base_salary        NUMERIC(15,2),
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

- [ ] `PartyCreated.java` — `record PartyCreated(Long partyId, String companyName, boolean isCustomer, boolean isSupplier, boolean isSalesperson) {}`
- [ ] `PartyDeactivated.java` — `record PartyDeactivated(Long partyId) {}`

---

## 3. Domain model

### Enums

- [ ] `party/domain/model/PersonType.java` — `LEGAL_ENTITY`, `INDIVIDUAL` (stored as `VARCHAR`)
- [ ] `party/domain/model/ContactType.java` — `PHONE`, `EMAIL`, `WHATSAPP`, `OTHER` (stored as `VARCHAR`)

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
  - Fields: `personType`, `cnpj`, `cpf`, `companyName`, `tradeName`, `isCustomer`, `isSupplier`, `isSalesperson`, `commissionPercent`, `baseSalary`, `active`
  - `@OneToMany(mappedBy = "partyId", cascade = CascadeType.ALL, orphanRemoval = true)` for addresses and contacts
  - `protected Party() {}` for JPA
  - `static Party.create(PersonType, cnpj, cpf, companyName, tradeName, isCustomer, isSupplier, isSalesperson, commissionPercent, baseSalary)` — `LEGAL_ENTITY` requires cnpj + companyName; `INDIVIDUAL` requires cpf + companyName; at least one role flag must be true; registers `PartyCreated`
  - `void updateProfile(String companyName, String tradeName, BigDecimal commissionPercent, BigDecimal baseSalary)`
  - `void deactivate()` — guard already inactive; registers `PartyDeactivated`
  - `Address addAddress(String street, String neighborhood, String zipCode, String number, String complement, Long cityId)`
  - `void removeAddress(Long addressId)` — guard: throw `IllegalStateException` if not found in collection
  - `Contact addContact(ContactType classification, String description)`
  - `void removeContact(Long contactId)`

---

## 4. Repositories

- [ ] `party/domain/repository/PartyRepository.java`
  - `JpaRepository<Party, Long>`
  - `Page<Party> findAllByIsCustomerTrue(Pageable)`
  - `Page<Party> findAllByIsSupplierTrue(Pageable)`
  - `Page<Party> findAllByIsSalespersonTrue(Pageable)`
  - `Page<Party> findByCompanyNameContainingIgnoreCase(String name, Pageable)`

---

## 5. Verification

- [ ] `./gradlew assemble` — compiles without errors
- [ ] `ModularStructureTest` passes — `party` does not import from other modules' sub-packages
