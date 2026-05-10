package github.io.ddmfuhrmann.outfit.catalog;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeResponse;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class SizeControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void createSizeReturns201() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("P"), headers), SizeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("P");
    }

    @Test
    void listSizesReturns200() {
        HttpHeaders headers = authHeaders(rest);
        rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("M"), headers), SizeResponse.class);
        var response = rest.exchange("/catalog/sizes?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content", "totalElements");
    }

    @Test
    void getSizeReturns200() {
        HttpHeaders headers = authHeaders(rest);
        SizeResponse created = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("G"), headers), SizeResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/sizes/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), SizeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("G");
    }

    @Test
    void getSizeNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/sizes/999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void renameSizeReturns200() {
        HttpHeaders headers = authHeaders(rest);
        SizeResponse created = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("GG"), headers), SizeResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/sizes/" + created.id(), HttpMethod.PUT,
                new HttpEntity<>(new SizeRequest("XGG"), headers), SizeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("XGG");
    }

    @Test
    void deleteSizeReturns204() {
        HttpHeaders headers = authHeaders(rest);
        SizeResponse created = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("PP"), headers), SizeResponse.class).getBody();
        assertThat(created).isNotNull();

        var deleteResponse = rest.exchange("/catalog/sizes/" + created.id(), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResponse = rest.exchange("/catalog/sizes/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteSizeNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/sizes/999999", HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createSizeWithBlankDescriptionReturns400() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("  "), headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
