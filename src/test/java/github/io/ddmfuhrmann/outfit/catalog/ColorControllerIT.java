package github.io.ddmfuhrmann.outfit.catalog;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorResponse;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class ColorControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void createColorReturns201() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest("Azul"), headers), ColorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Azul");
    }

    @Test
    void listColorsReturns200() {
        HttpHeaders headers = authHeaders(rest);
        rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest("Vermelho"), headers), ColorResponse.class);
        var response = rest.exchange("/catalog/colors?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content", "totalElements");
    }

    @Test
    void getColorReturns200() {
        HttpHeaders headers = authHeaders(rest);
        ColorResponse created = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest("Verde"), headers), ColorResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/colors/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), ColorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Verde");
    }

    @Test
    void getColorNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/colors/999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void renameColorReturns200() {
        HttpHeaders headers = authHeaders(rest);
        ColorResponse created = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest("Preto"), headers), ColorResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/colors/" + created.id(), HttpMethod.PUT,
                new HttpEntity<>(new ColorRequest("Branco"), headers), ColorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Branco");
    }

    @Test
    void deleteColorReturns204() {
        HttpHeaders headers = authHeaders(rest);
        ColorResponse created = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest("Rosa"), headers), ColorResponse.class).getBody();
        assertThat(created).isNotNull();

        var deleteResponse = rest.exchange("/catalog/colors/" + created.id(), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResponse = rest.exchange("/catalog/colors/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteColorNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/colors/999999", HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createColorWithBlankDescriptionReturns400() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest(""), headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
