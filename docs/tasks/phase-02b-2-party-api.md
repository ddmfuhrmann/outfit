# Tasks — Phase 2b-2: Party API

DTOs, use cases, controller e testes para o módulo `party`.
Cobre as operações de escrita (create, update, deactivate, add/remove address e contact).
Os endpoints de leitura (`GET /party`, `GET /party/{id}`) são implementados no task phase-02b-3-query-module.md.

Depends on: docs/tasks/phase-02b-1-domain.md

---

## 1. DTOs

### Requests

- [ ] `CreatePartyRequest.java` — record com todos os campos de party + listas opcionais de endereços e contatos iniciais
- [ ] `UpdatePartyRequest.java` — record com campos de perfil atualizáveis (`legalName`, `name`, `commissionPercent`)
- [ ] `AddAddressRequest.java` — `record AddAddressRequest(String street, String neighborhood, String zipCode, String number, String complement, Long cityId) {}`
- [ ] `AddContactRequest.java` — `record AddContactRequest(ContactType classification, String description) {}`

### Responses

- [ ] `PartyCreatedResponse.java` — `record PartyCreatedResponse(Long id) {}` — returned from `POST /party` so callers know the new id

---

## 2. Use cases

- [ ] `CreatePartyUseCase` — chama `Party.create(...)`, salva; se a request incluir endereços ou contatos iniciais chama os métodos do aggregate antes do save; retorna `PartyCreatedResponse`
- [ ] `UpdatePartyUseCase` — carrega → `party.updateProfile(...)` → dirty checking (sem `save()` explícito)
- [ ] `DeactivatePartyUseCase` — carrega → `party.deactivate()`
- [ ] `AddAddressUseCase` — carrega party → `party.addAddress(...)` → dirty checking
- [ ] `RemoveAddressUseCase` — carrega party → `party.removeAddress(addressId)`
- [ ] `AddContactUseCase` — carrega party → `party.addContact(...)`
- [ ] `RemoveContactUseCase` — carrega party → `party.removeContact(contactId)`

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
  - Criar `LEGAL_ENTITY` — `400` se `cnpj` ausente
  - Criar `INDIVIDUAL` — `400` se `cpf` ausente
  - Criar party sem nenhum flag de papel — `400`
  - `POST /party` retorna `201` com `id` no body
  - `PUT /party/{id}` retorna `204`; `PartyUpdated` registrado no aggregate
  - `DELETE /party/{id}` retorna `204`; `PartyDeactivated` registrado no aggregate
  - Desativar party já inativo — `422`
  - `POST /party/{id}/addresses` retorna `204`; `PartyAddressAdded` registrado
  - `DELETE /party/{id}/addresses/{addressId}` retorna `204`; `PartyAddressRemoved` registrado
  - `POST /party/{id}/contacts` retorna `204`; `PartyContactAdded` registrado
  - `DELETE /party/{id}/contacts/{contactId}` retorna `204`; `PartyContactRemoved` registrado
  - `PartyCreated` registrado no aggregate após `POST /party`

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — todos os ITs passam
- [ ] `ModularStructureTest` passa
- [ ] `GET /docs` — endpoints de escrita de party visíveis no OpenAPI
