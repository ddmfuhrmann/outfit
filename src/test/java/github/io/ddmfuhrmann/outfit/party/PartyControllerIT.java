package github.io.ddmfuhrmann.outfit.party;

import github.io.ddmfuhrmann.outfit.party.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.domain.model.ContactType;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.party.domain.repository.PartyRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PartyControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    PartyRepository partyRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    // --- valid test data ---

    private static final String VALID_CNPJ = "11222333000181";
    private static final String VALID_CPF  = "52998224725";

    private CreatePartyRequest legalEntityRequest() {
        return new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, VALID_CNPJ, null,
                "Acme Ltda", "Acme",
                true, false, false,
                null, null, null);
    }

    private CreatePartyRequest individualRequest() {
        return new CreatePartyRequest(
                PersonType.INDIVIDUAL, null, VALID_CPF,
                "João da Silva", "João",
                true, false, false,
                null, null, null);
    }

    private Long createParty() {
        var response = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(legalEntityRequest(), authHeaders(rest)), PartyCreatedResponse.class);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().id();
    }

    // --- validation tests ---

    @Test
    void createLegalEntityWithoutCnpjReturns400() {
        var request = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, null, null,
                "Missing CNPJ Ltda", null,
                true, false, false,
                null, null, null);
        var response = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(rest)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createIndividualWithoutCpfReturns400() {
        var request = new CreatePartyRequest(
                PersonType.INDIVIDUAL, null, null,
                "Missing CPF", null,
                true, false, false,
                null, null, null);
        var response = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(rest)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createPartyWithNoRoleReturns400() {
        var request = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, VALID_CNPJ, null,
                "No Role Ltda", null,
                false, false, false,
                null, null, null);
        var response = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(rest)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- create ---

    @Test
    void createPartyReturns201WithId() {
        var response = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(legalEntityRequest(), authHeaders(rest)), PartyCreatedResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isPositive();
    }

    @Test
    void createIndividualPartyReturns201WithId() {
        var response = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(individualRequest(), authHeaders(rest)), PartyCreatedResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isPositive();
    }

    // --- update ---

    @Test
    void updatePartyReturns204() {
        Long id = createParty();
        var update = new UpdatePartyRequest("Acme Updated Ltda", "Acme Updated", null);
        var response = rest.exchange("/party/" + id, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders(rest)), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String legalName = transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().getLegalName());
        assertThat(legalName).isEqualTo("Acme Updated Ltda");
    }

    // --- deactivate ---

    @Test
    void deactivatePartyReturns204() {
        Long id = createParty();
        var response = rest.exchange("/party/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        boolean active = Boolean.TRUE.equals(transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().isActive()));
        assertThat(active).isFalse();
    }

    @Test
    void deactivateAlreadyInactivePartyReturns422() {
        Long id = createParty();
        rest.exchange("/party/" + id, HttpMethod.DELETE, new HttpEntity<>(authHeaders(rest)), Void.class);

        var response = rest.exchange("/party/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // --- addresses ---

    @Test
    void addAddressReturns204() {
        Long id = createParty();
        var req = new AddAddressRequest("Rua A", "Centro", "01310100", "10", null, null);
        var response = rest.exchange("/party/" + id + "/addresses", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer addressCount = transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().getAddresses().size());
        assertThat(addressCount).isEqualTo(1);
    }

    @Test
    void removeAddressReturns204() {
        Long id = createParty();
        var req = new AddAddressRequest("Rua B", "Vila", "01310200", "20", null, null);
        rest.exchange("/party/" + id + "/addresses", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);

        Long addressId = transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().getAddresses().getFirst().getId());

        var response = rest.exchange("/party/" + id + "/addresses/" + addressId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer addressCount = transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().getAddresses().size());
        assertThat(addressCount).isZero();
    }

    // --- contacts ---

    @Test
    void addContactReturns204() {
        Long id = createParty();
        var req = new AddContactRequest(ContactType.EMAIL, "contato@acme.com");
        var response = rest.exchange("/party/" + id + "/contacts", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer contactCount = transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().getContacts().size());
        assertThat(contactCount).isEqualTo(1);
    }

    @Test
    void removeContactReturns204() {
        Long id = createParty();
        var req = new AddContactRequest(ContactType.PHONE, "11999999999");
        rest.exchange("/party/" + id + "/contacts", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(rest)), Void.class);

        Long contactId = transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().getContacts().getFirst().getId());

        var response = rest.exchange("/party/" + id + "/contacts/" + contactId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer contactCount = transactionTemplate.execute(tx ->
                partyRepository.findById(id).orElseThrow().getContacts().size());
        assertThat(contactCount).isZero();
    }

}
