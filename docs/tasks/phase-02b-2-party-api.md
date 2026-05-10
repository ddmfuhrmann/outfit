# Tasks — Phase 2b-2: Party API

DTOs, use cases, controller, and tests for the `party` module.
Covers write operations (create, update, deactivate, add/remove address and contact).
Read endpoints (`GET /party`, `GET /party/{id}`) are implemented in phase-02b-3-query-module.md.

Depends on: docs/tasks/phase-02b-1-domain.md

---

## 1. DTOs

### Requests

- [ ] `CreatePartyRequest.java` — record with all party fields + optional lists of initial addresses and contacts
- [ ] `UpdatePartyRequest.java` — record with updatable profile fields (`legalName`, `name`, `commissionPercent`)
- [ ] `AddAddressRequest.java` — `record AddAddressRequest(String street, String neighborhood, String zipCode, String number, String complement, Long cityId) {}`
- [ ] `AddContactRequest.java` — `record AddContactRequest(ContactType classification, String description) {}`

### Responses

- [ ] `PartyCreatedResponse.java` — `record PartyCreatedResponse(Long id) {}` — returned from `POST /party` so callers know the new id

---

## 2. Use cases

- [ ] `CreatePartyUseCase` — calls `Party.create(...)`, saves; if the request includes initial addresses or contacts, calls the aggregate methods after save; returns `PartyCreatedResponse`
- [ ] `UpdatePartyUseCase` — loads → `party.updateProfile(...)` → dirty checking (no explicit `save()`)
- [ ] `DeactivatePartyUseCase` — loads → `party.deactivate()`
- [ ] `AddAddressUseCase` — loads party → `party.addAddress(...)` → dirty checking
- [ ] `RemoveAddressUseCase` — loads party → `party.removeAddress(addressId)`
- [ ] `AddContactUseCase` — loads party → `party.addContact(...)`
- [ ] `RemoveContactUseCase` — loads party → `party.removeContact(contactId)`

---

## 3. Controller

- [ ] `party/api/rest/PartyController.java`
  - `POST /party` → `CreatePartyUseCase` — returns `201 Created` with `PartyCreatedResponse`
  - `PUT /party/{id}` → `UpdatePartyUseCase` — returns `204 No Content`
  - `DELETE /party/{id}` → `DeactivatePartyUseCase` — returns `204 No Content`
  - `POST /party/{id}/addresses` → `AddAddressUseCase` — returns `204 No Content`
  - `DELETE /party/{id}/addresses/{addressId}` → `RemoveAddressUseCase` — returns `204 No Content`
  - `POST /party/{id}/contacts` → `AddContactUseCase` — returns `204 No Content`
  - `DELETE /party/{id}/contacts/{contactId}` → `RemoveContactUseCase` — returns `204 No Content`

---

## 4. Tests

- [ ] `PartyControllerIT`
  - Create `LEGAL_ENTITY` — `400` if `cnpj` is absent
  - Create `INDIVIDUAL` — `400` if `cpf` is absent
  - Create party with no role flag set — `400`
  - `POST /party` returns `201` with `id` in body
  - `PUT /party/{id}` returns `204`; `PartyUpdated` registered in aggregate
  - `DELETE /party/{id}` returns `204`; `PartyDeactivated` registered in aggregate
  - Deactivate already-inactive party — `422`
  - `POST /party/{id}/addresses` returns `204`; `PartyAddressAdded` registered
  - `DELETE /party/{id}/addresses/{addressId}` returns `204`; `PartyAddressRemoved` registered
  - `POST /party/{id}/contacts` returns `204`; `PartyContactAdded` registered
  - `DELETE /party/{id}/contacts/{contactId}` returns `204`; `PartyContactRemoved` registered
  - `PartyCreated` registered in aggregate after `POST /party`

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — all ITs pass
- [ ] `ModularStructureTest` passes
- [ ] `GET /docs` — party write endpoints visible in OpenAPI
