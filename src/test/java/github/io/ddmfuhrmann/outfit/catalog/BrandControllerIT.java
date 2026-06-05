package github.io.ddmfuhrmann.outfit.catalog;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class BrandControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    // --- helpers ---

    private Long createBrand(HttpHeaders headers, String description) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest(description), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSupplierParty(HttpHeaders headers, String cnpj) {
        var req = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, cnpj, null,
                "Fornecedora Nacional S.A.", "Fornecedora Nacional",
                false, true, false,
                null, null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, headers), PartyCreatedResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCustomerOnlyParty(HttpHeaders headers, String cnpj) {
        var req = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, cnpj, null,
                "Cliente Nacional S.A.", "Cliente Nacional",
                true, false, false,
                null, null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, headers), PartyCreatedResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    // --- existing tests ---

    @Test
    void createBrandReturns201() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Nike"), headers), BrandResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Nike");
    }

    @Test
    void listBrandsReturns200() {
        HttpHeaders headers = authHeaders(rest);
        rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Adidas"), headers), BrandResponse.class);
        var response = rest.exchange("/catalog/brands?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content", "totalElements");
    }

    @Test
    void getBrandReturns200() {
        HttpHeaders headers = authHeaders(rest);
        BrandResponse created = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Puma"), headers), BrandResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/brands/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), BrandResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Puma");
    }

    @Test
    void getBrandNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/brands/999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void renameBrandReturns200() {
        HttpHeaders headers = authHeaders(rest);
        BrandResponse created = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Reebok"), headers), BrandResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/brands/" + created.id(), HttpMethod.PUT,
                new HttpEntity<>(new BrandRequest("New Balance"), headers), BrandResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("New Balance");
    }

    @Test
    void deleteBrandReturns204() {
        HttpHeaders headers = authHeaders(rest);
        BrandResponse created = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Vans"), headers), BrandResponse.class).getBody();
        assertThat(created).isNotNull();

        var deleteResponse = rest.exchange("/catalog/brands/" + created.id(), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResponse = rest.exchange("/catalog/brands/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteBrandNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/brands/999999", HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createBrandWithBlankDescriptionReturns400() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("  "), headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- supplier endpoint tests ---

    @Test
    void addSupplierToBrandReturns200() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Brand-AddSupplier");
        Long supplierId = createSupplierParty(headers, "45543915000181");

        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.POST, new HttpEntity<>(headers), BrandResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().supplierIds()).contains(supplierId);
    }

    @Test
    void addSupplierBrandNotFoundReturns404() {
        var headers = authHeaders(rest);
        Long supplierId = createSupplierParty(headers, "83695003000114");

        var resp = rest.exchange("/catalog/brands/999999/suppliers/" + supplierId,
                HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addNonExistentSupplierReturns404() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Brand-NonExistSupplier");

        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/999999",
                HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addNonSupplierPartyReturns400() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Brand-NonSupplier");
        Long customerId = createCustomerOnlyParty(headers, "92303004000167");

        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + customerId,
                HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addDuplicateSupplierReturns422() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Brand-DupSupplier");
        Long supplierId = createSupplierParty(headers, "75821506000160");

        rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.POST, new HttpEntity<>(headers), BrandResponse.class);

        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void removeSupplierFromBrandReturns200() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Brand-RemoveSupplier");
        Long supplierId = createSupplierParty(headers, "26576455000143");

        rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.POST, new HttpEntity<>(headers), BrandResponse.class);

        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.DELETE, new HttpEntity<>(headers), BrandResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().supplierIds()).doesNotContain(supplierId);
    }

    @Test
    void removeAbsentSupplierReturns422() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Brand-RemoveAbsent");
        Long supplierId = createSupplierParty(headers, "98765432000198");

        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

}
