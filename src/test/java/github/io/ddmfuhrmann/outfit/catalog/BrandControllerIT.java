package github.io.ddmfuhrmann.outfit.catalog;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class BrandControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void createBrandReturns201() {
        HttpHeaders headers = authHeaders();
        var response = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Nike"), headers), BrandResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Nike");
    }

    @Test
    void listBrandsReturns200() {
        HttpHeaders headers = authHeaders();
        rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Adidas"), headers), BrandResponse.class);
        var response = rest.exchange("/catalog/brands?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content", "totalElements");
    }

    @Test
    void getBrandReturns200() {
        HttpHeaders headers = authHeaders();
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
        HttpHeaders headers = authHeaders();
        var response = rest.exchange("/catalog/brands/999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void renameBrandReturns200() {
        HttpHeaders headers = authHeaders();
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
        HttpHeaders headers = authHeaders();
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
        HttpHeaders headers = authHeaders();
        var response = rest.exchange("/catalog/brands/999999", HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createBrandWithBlankDescriptionReturns400() {
        HttpHeaders headers = authHeaders();
        var response = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("  "), headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpHeaders authHeaders() {
        String token = rest.postForObject("/auth/login",
                new LoginRequest("admin", "admin"), LoginResponse.class).token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
