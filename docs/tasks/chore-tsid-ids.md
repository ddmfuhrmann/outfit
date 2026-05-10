# Chore — TSID client-generated IDs

Substituir `BIGSERIAL` + `GenerationType.IDENTITY` por TSIDs gerados em Java, eliminando o padrão
`saveAndFlush → onXxxAdded → save` que existe hoje nos use cases de child-entities.

**Motivação:** com `IDENTITY`, o ID da entidade filha só existe após o INSERT no banco, então o
evento não pode ser registrado dentro do método de domínio. Isso força métodos auxiliares
(`onAddressAdded`, `onContactAdded`) e um double-save nos use cases. Com TSID o ID é gerado no
construtor, antes do persist, e o evento pode ser registrado diretamente no método de domínio.

---

## 1. Dependência

- [ ] `build.gradle.kts` — adicionar `com.github.f4b6a3:tsid-creator:<latest>`

---

## 2. Classes base

- [ ] `shared/domain/model/BaseEntity.java`
  - Remover `@GeneratedValue(strategy = GenerationType.IDENTITY)`
  - Gerar ID no construtor protegido: `this.id = TsidCreator.getTsid().toLong()`
- [ ] `shared/domain/model/BaseAggregate.java` — mesma mudança

---

## 3. Flyway migrations

Trocar `BIGSERIAL` → `BIGINT` em todas as tabelas (o Postgres não precisa mais gerar o ID).
Como o projeto é pré-produção, editar as migrations existentes e recriar o schema.

- [ ] `V1__shared_schema.sql` — `company`, `city`, `app_user`: `BIGSERIAL` → `BIGINT`
- [ ] `V6__catalog_reference_schema.sql` — todas as tabelas de referência: `BIGSERIAL` → `BIGINT`
- [ ] `V7__party_schema.sql` — `party`, `party_address`, `party_contact`: `BIGSERIAL` → `BIGINT`
- [ ] Demais migrations futuras devem usar `BIGINT` direto

---

## 4. Party domain model

- [ ] Remover `@PostPersist onPersisted()` de `Party`
  - Registrar `PartyCreated` no final de `Builder.build()` (ID já disponível no construtor)
- [ ] `Party.addAddress()` — registrar `PartyAddressAdded` diretamente (address já tem ID)
- [ ] `Party.addContact()` — registrar `PartyContactAdded` diretamente (contact já tem ID)
- [ ] Remover `Party.onAddressAdded()`
- [ ] Remover `Party.onContactAdded()`

---

## 5. Use cases simplificados

- [ ] `AddAddressUseCase` — substituir `saveAndFlush + onAddressAdded + save` por único `save`
- [ ] `AddContactUseCase` — substituir `saveAndFlush + onContactAdded + save` por único `save`

---

## 6. Outros aggregates/entities

Entidades em `shared` e `catalog` herdam `BaseEntity`/`BaseAggregate` e passam a receber TSID
automaticamente após o passo 2 — nenhuma mudança de comportamento adicional necessária nelas,
apenas garantir que não usam `@PostPersist` ou `saveAndFlush` pelo mesmo motivo.

---

## 7. Testes

- [ ] Rodar `./gradlew test` e garantir que todos os testes passam com o novo esquema
- [ ] Verificar `ModularStructureTest`
