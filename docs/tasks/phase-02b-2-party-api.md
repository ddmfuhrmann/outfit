# Tasks — Phase 2b-2: Party API

DTOs, use cases, controller e testes para o módulo `party`.
Cobre o CRUD completo de party mais gerenciamento aninhado de endereços e contatos.

Depends on: docs/tasks/phase-02b-1-domain.md

---

## 1. DTOs

### Requests

- [ ] `CreatePartyRequest.java` — record com todos os campos de party + listas opcionais de endereços e contatos iniciais
- [ ] `UpdatePartyRequest.java` — record com campos de perfil atualizáveis (`companyName`, `tradeName`, `commissionPercent`, `baseSalary`)
- [ ] `AddAddressRequest.java` — `record AddAddressRequest(String street, String neighborhood, String zipCode, String number, String complement, Long cityId) {}`
- [ ] `AddContactRequest.java` — `record AddContactRequest(ContactType classification, String description) {}`

### Responses

- [ ] `AddressResponse.java` — `static from(Address, String cityName)` (cityName enriquecido pelo use case via lookup na tabela `city`)
- [ ] `ContactResponse.java` — `static from(Contact)`
- [ ] `PartyResponse.java` — party completo com listas de `AddressResponse` e `ContactResponse`; `static from(Party, List<AddressResponse>, List<ContactResponse>)`
- [ ] `PartySummaryResponse.java` — `id`, `companyName`, `personType`, flags de papel, `active` (para o endpoint de listagem)

---

## 2. Use cases

- [ ] `CreatePartyUseCase` — chama `Party.create(...)`, salva; se a request incluir endereços ou contatos iniciais chama os métodos do aggregate antes do save
- [ ] `GetPartyUseCase` — carrega party + enriquece endereços com nome da cidade via `JdbcTemplate` ou query direta em `city`
- [ ] `ListPartiesUseCase` — aceita filtro de papel (`customer | supplier | salesperson | all`) + busca opcional por nome; retorna `PageResponse<PartySummaryResponse>`
- [ ] `UpdatePartyUseCase` — carrega → `party.updateProfile(...)` → dirty checking (sem `save()` explícito)
- [ ] `DeactivatePartyUseCase` — carrega → `party.deactivate()`
- [ ] `AddAddressUseCase` — carrega party → `party.addAddress(...)` → dirty checking
- [ ] `RemoveAddressUseCase` — carrega party → `party.removeAddress(addressId)`
- [ ] `AddContactUseCase` — carrega party → `party.addContact(...)`
- [ ] `RemoveContactUseCase` — carrega party → `party.removeContact(contactId)`

---

## 3. Controller

- [ ] `party/api/rest/PartyController.java`
  - `POST /party` → `CreatePartyUseCase`
  - `GET /party` → `ListPartiesUseCase` (query params: `?role=customer|supplier|salesperson`, `?name=`)
  - `GET /party/{id}` → `GetPartyUseCase`
  - `PUT /party/{id}` → `UpdatePartyUseCase`
  - `DELETE /party/{id}` → `DeactivatePartyUseCase`
  - `POST /party/{id}/addresses` → `AddAddressUseCase`
  - `DELETE /party/{id}/addresses/{addressId}` → `RemoveAddressUseCase`
  - `POST /party/{id}/contacts` → `AddContactUseCase`
  - `DELETE /party/{id}/contacts/{contactId}` → `RemoveContactUseCase`

---

## 4. Tests

- [ ] `PartyControllerIT`
  - Criar `LEGAL_ENTITY` — `400` se `cnpj` ausente
  - Criar `INDIVIDUAL` — `400` se `cpf` ausente
  - Criar party sem nenhum flag de papel — `400`
  - Listar com filtro `?role=customer` — retorna apenas registros correspondentes
  - Busca por nome com `?name=`
  - Adicionar endereço e contato; ambos aparecem em `GET /party/{id}`
  - Remover endereço e contato; nenhum aparece no `GET` seguinte
  - Desativar party — `GET` subsequente mostra `active: false`
  - Desativar party já inativo — `422`
  - `PartyCreated` e `PartyDeactivated` registrados no aggregate

---

## 5. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — todos os ITs passam
- [ ] `ModularStructureTest` passa
- [ ] `GET /docs` — endpoints de party visíveis no OpenAPI
