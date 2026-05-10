# Chore — Auth header utility for integration tests

Extrair o método `authHeaders()` duplicado em 6 classes de teste para um utilitário singleton em
`AbstractIT`, eliminando a chamada a `/auth/login` repetida a cada teste.

**Motivação:** cada `*ControllerIT` tem uma cópia idêntica de `authHeaders()` que faz POST em
`/auth/login` a cada invocação. Com o método no `AbstractIT` e o token cacheado como campo
estático, o login ocorre uma única vez por ciclo de testes.

---

## 1. Utilitário em AbstractIT

- [ ] `shared/AbstractIT.java` — adicionar campo estático e método de suporte:
  ```java
  private static String cachedToken;

  protected HttpHeaders authHeaders(TestRestTemplate rest) {
      if (cachedToken == null) {
          cachedToken = rest.postForObject("/auth/login",
                  new LoginRequest("admin", "admin"), LoginResponse.class).token();
      }
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(cachedToken);
      headers.setContentType(MediaType.APPLICATION_JSON);
      return headers;
  }
  ```
  - `cachedToken` é `static` para sobreviver entre instâncias da subclasse dentro do mesmo
    contexto Spring (todas as `*ControllerIT` compartilham o mesmo `ApplicationContext`)
  - `TestRestTemplate` é injetado pelas subclasses e passado como argumento porque `AbstractIT`
    não o instancia diretamente

---

## 2. Remoção das cópias locais

Remover o método `private HttpHeaders authHeaders()` de cada classe e substituir as chamadas por
`authHeaders(rest)`:

- [ ] `catalog/BrandControllerIT.java`
- [ ] `catalog/CategoryControllerIT.java`
- [ ] `catalog/ColorControllerIT.java`
- [ ] `catalog/SizeControllerIT.java`
- [ ] `party/PartyControllerIT.java`
- [ ] `query/PartyQueryControllerIT.java`

---

## 3. Verificação

- [ ] `./gradlew test` — todos os testes passam
- [ ] Confirmar via log ou breakpoint que `/auth/login` é chamado uma única vez por suite
