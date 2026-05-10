# Tasks — Phase 2b-3: Party Query Module

Índice Elasticsearch de parties no módulo `query`: listeners de eventos, use cases de indexação,
use cases de leitura e controller. Toda leitura de party (`GET /party`, `GET /party/{id}`) é servida pelo Elasticsearch.

Depends on: docs/tasks/phase-02b-2-party-api.md

---

## 1. Document records

- [ ] `query/application/dto/AddressDocument.java`
  ```java
  record AddressDocument(Long id, String street, String neighborhood, String zipCode,
                         String number, String complement, Long cityId, String cityName) {}
  ```
- [ ] `query/application/dto/ContactDocument.java`
  ```java
  record ContactDocument(Long id, String classification, String description) {}
  ```
- [ ] `query/application/dto/PartyDocument.java`
  - Fields: `id`, `personType`, `cnpj`, `cpf`, `legalName`, `name`, `customer`, `supplier`, `salesperson`, `commissionPercent`, `active`, `createdAt`, `updatedAt`
  - Nested: `List<AddressDocument> addresses`, `List<ContactDocument> contacts`

---

## 2. Read-only query repository

- [ ] `query/infrastructure/persistence/PartyQueryRepository.java`
  - Acessa as tabelas `party`, `party_address`, `party_contact`, `city` diretamente via `JdbcTemplate` (sem importar nenhuma classe de `party.*`)
  - Método principal: `PartyDocument findFullById(Long partyId)` — retorna o documento enriquecido com nome da cidade em cada endereço; lança `ResourceNotFoundException` se o party não existir

---

## 3. Event listener

- [ ] `query/application/listener/PartyIndexListener.java`
  - `@ApplicationModuleListener` — not `@Transactional`
  - Handles `PartyCreated`, `PartyUpdated`, `PartyDeactivated`, `PartyAddressAdded`, `PartyAddressRemoved`, `PartyContactAdded`, `PartyContactRemoved`
  - All events delegate to `IndexPartyUseCase` passing `partyId` — the use case always reloads and re-indexes the full document
  - **Synchronous replication:** `EventPublicationConfig` (shared) provides `SyncTaskExecutor`, so listeners run in the same request thread after the Postgres commit. The HTTP response is only sent after ES indexing completes — no eventual consistency lag for Party.

---

## 4. Indexing use case

- [ ] `query/application/usecase/IndexPartyUseCase`
  - Carrega documento completo via `PartyQueryRepository.findFullById(partyId)`
  - Faz upsert no índice `parties` via `ElasticsearchClient` (dynamic mapping — sem definição de mapping explícita)
  - Em `PartyDeactivated`: recarrega e re-indexa com `active: false` (o repositório já reflete o estado persistido)

---

## 5. Read use cases

- [ ] `query/application/usecase/SearchPartiesUseCase`
  - Aceita `String q` (nullable), `String role` (nullable: `customer | supplier | salesperson`), `Pageable`
  - `q` → multi-match query em `legalName`, `name`, `cnpj`, `cpf`
  - `role` → term filter: `customer` → `customer: true`, `supplier` → `supplier: true`, `salesperson` → `salesperson: true`
  - Retorna `PageResponse<PartyDocument>`

- [ ] `query/application/usecase/GetPartyByIdUseCase`
  - Busca documento por `id` no Elasticsearch; lança `ResourceNotFoundException` (mapeado para `404`) se ausente

---

## 6. Controller

- [ ] `query/api/rest/PartyQueryController.java`
  - `GET /party?q=&role=` → `SearchPartiesUseCase`
    - OpenAPI: "Served from Elasticsearch. Reflects committed state synchronously — domain events are indexed before the write response is sent."
  - `GET /party/{id}` → `GetPartyByIdUseCase`
    - OpenAPI: mesma nota de consistência síncrona

---

## 7. Tests

- [ ] `PartyQueryControllerIT` (Testcontainers Elasticsearch)
  - Após evento `PartyCreated`, documento indexado e `GET /party/{id}` retorna o party completo
  - `GET /party?q=<legalName>` retorna documentos correspondentes
  - `GET /party?role=customer` retorna apenas parties com `customer: true`
  - Após `PartyUpdated`, documento reflete os novos valores de perfil
  - Após `PartyDeactivated`, documento mostra `active: false`
  - Após `PartyAddressAdded`, endereço (com `cityName`) aparece em `addresses` do documento
  - Após `PartyAddressRemoved`, endereço não aparece mais em `addresses`
  - Após `PartyContactAdded` / `PartyContactRemoved`, `contacts` reflete o estado correto
  - `GET /party/{id}` para id inexistente retorna `404`

---

## 8. Verification

- [ ] `./gradlew build` — green
- [ ] `./gradlew test` — todos os ITs passam incluindo Testcontainers ES
- [ ] `ModularStructureTest` passa — `query` não importa sub-pacotes de `party`
- [ ] `GET /docs` — endpoints de leitura visíveis com nota de eventual consistency
