package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.party.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.domain.model.ContactType;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.query.application.dto.PartyDocument;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class PartyQueryControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private static final String VALID_CNPJ = "11222333000181";
    private static final String VALID_CPF  = "52998224725";

    private Long createLegalEntity() {
        var req = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, VALID_CNPJ, null,
                "Acme Ltda", "Acme",
                true, false, false,
                null, null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), PartyCreatedResponse.class);
        assertThat(resp.getBody()).isNotNull();
        return resp.getBody().id();
    }

    private Long createCustomer() {
        var req = new CreatePartyRequest(
                PersonType.INDIVIDUAL, null, VALID_CPF,
                "João da Silva", "João",
                true, false, false,
                null, null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), PartyCreatedResponse.class);
        assertThat(resp.getBody()).isNotNull();
        return resp.getBody().id();
    }

    @Test
    void afterPartyCreated_getByIdReturnsFullDocument() {
        Long id = createLegalEntity();

        var resp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isEqualTo(id);
        assertThat(resp.getBody().legalName()).isEqualTo("Acme Ltda");
        assertThat(resp.getBody().active()).isTrue();
        assertThat(resp.getBody().customer()).isTrue();
    }

    @Test
    void searchByLegalName_returnsMatchingDocuments() {
        Long id = createLegalEntity();

        var resp = rest.exchange("/party?q=Acme+Ltda", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)),
                new ParameterizedTypeReference<PageResponse<PartyDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().content())
                .extracting(PartyDocument::id)
                .contains(id);
    }

    @Test
    void filterByRoleCustomer_returnsOnlyCustomers() {
        Long id = createCustomer();

        var resp = rest.exchange("/party?role=customer", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)),
                new ParameterizedTypeReference<PageResponse<PartyDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().content()).isNotEmpty();
        assertThat(resp.getBody().content()).allMatch(PartyDocument::customer);
        assertThat(resp.getBody().content()).extracting(PartyDocument::id).contains(id);
    }

    @Test
    void afterPartyUpdated_documentReflectsNewValues() {
        Long id = createLegalEntity();

        var update = new UpdatePartyRequest("Acme Updated Ltda", "Acme Updated", null);
        rest.exchange("/party/" + id, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders(rest)), Void.class);

        var resp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().legalName()).isEqualTo("Acme Updated Ltda");
    }

    @Test
    void afterPartyDeactivated_documentShowsActiveFalse() {
        Long id = createLegalEntity();

        rest.exchange("/party/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);

        var resp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().active()).isFalse();
    }

    @Test
    void afterAddressAdded_addressAppearsWithCityName() {
        Long id = createLegalEntity();

        var req = new AddAddressRequest("Rua A", "Centro", "01310100", "10", null, null);
        rest.exchange("/party/" + id + "/addresses", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);

        var resp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().addresses()).hasSize(1);
        assertThat(resp.getBody().addresses().getFirst().street()).isEqualTo("Rua A");
    }

    @Test
    void afterAddressRemoved_addressDisappearsFromDocument() {
        Long id = createLegalEntity();

        var req = new AddAddressRequest("Rua B", "Vila", "01310200", "20", null, null);
        rest.exchange("/party/" + id + "/addresses", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);

        var docResp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);
        assertThat(docResp.getBody()).isNotNull();
        Long addressId = docResp.getBody().addresses().getFirst().id();

        rest.exchange("/party/" + id + "/addresses/" + addressId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);

        var afterResp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);
        assertThat(afterResp.getBody()).isNotNull();
        assertThat(afterResp.getBody().addresses()).isEmpty();
    }

    @Test
    void afterContactAdded_contactAppearsInDocument() {
        Long id = createLegalEntity();

        var req = new AddContactRequest(ContactType.EMAIL, "contato@acme.com");
        rest.exchange("/party/" + id + "/contacts", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);

        var resp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().contacts()).hasSize(1);
        assertThat(resp.getBody().contacts().getFirst().description()).isEqualTo("contato@acme.com");
    }

    @Test
    void afterContactRemoved_contactDisappearsFromDocument() {
        Long id = createLegalEntity();

        var req = new AddContactRequest(ContactType.PHONE, "11999999999");
        rest.exchange("/party/" + id + "/contacts", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);

        var docResp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);
        assertThat(docResp.getBody()).isNotNull();
        Long contactId = docResp.getBody().contacts().getFirst().id();

        rest.exchange("/party/" + id + "/contacts/" + contactId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);

        var afterResp = rest.exchange("/party/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), PartyDocument.class);
        assertThat(afterResp.getBody()).isNotNull();
        assertThat(afterResp.getBody().contacts()).isEmpty();
    }

    @Test
    void searchByPrefix_returnsMatchingParty() {
        var req = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, VALID_CNPJ, null,
                "Prefixcorp Ltda", "Prefixcorp",
                false, true, false,
                null, null, null);
        var createResp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), PartyCreatedResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long id = createResp.getBody().id();

        var resp = rest.exchange("/party?q=Prefix", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)),
                new ParameterizedTypeReference<PageResponse<PartyDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().content())
                .extracting(PartyDocument::id)
                .contains(id);
    }

    @Test
    void getByIdForNonExistentPartyReturns404() {
        var resp = rest.exchange("/party/999999", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
