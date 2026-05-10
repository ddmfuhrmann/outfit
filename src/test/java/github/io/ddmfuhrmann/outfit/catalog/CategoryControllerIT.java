package github.io.ddmfuhrmann.outfit.catalog;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.CategoryRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.CategoryResponse;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void createCategoryReturns201() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Camisetas", "6109.10.00"), headers), CategoryResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Camisetas");
        assertThat(response.getBody().ncmCode()).isEqualTo("6109.10.00");
    }

    @Test
    void createCategoryWithoutNcmCodeReturns201() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Acessórios", null), headers), CategoryResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ncmCode()).isNull();
    }

    @Test
    void listCategoriesReturns200() {
        HttpHeaders headers = authHeaders(rest);
        rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Calças", null), headers), CategoryResponse.class);
        var response = rest.exchange("/catalog/categories?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content", "totalElements");
    }

    @Test
    void getCategoryReturns200() {
        HttpHeaders headers = authHeaders(rest);
        CategoryResponse created = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Bermudas", "6203.42.00"), headers), CategoryResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/categories/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), CategoryResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Bermudas");
    }

    @Test
    void getCategoryNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/categories/999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void renameCategoryReturns200() {
        HttpHeaders headers = authHeaders(rest);
        CategoryResponse created = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Meias", null), headers), CategoryResponse.class).getBody();
        assertThat(created).isNotNull();

        var response = rest.exchange("/catalog/categories/" + created.id(), HttpMethod.PUT,
                new HttpEntity<>(new CategoryRequest("Meias Esportivas", "6115.96.00"), headers), CategoryResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Meias Esportivas");
        assertThat(response.getBody().ncmCode()).isEqualTo("6115.96.00");
    }

    @Test
    void deleteCategoryReturns204() {
        HttpHeaders headers = authHeaders(rest);
        CategoryResponse created = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Bonés", null), headers), CategoryResponse.class).getBody();
        assertThat(created).isNotNull();

        var deleteResponse = rest.exchange("/catalog/categories/" + created.id(), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResponse = rest.exchange("/catalog/categories/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCategoryNotFoundReturns404() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/categories/999999", HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createCategoryWithBlankDescriptionReturns400() {
        HttpHeaders headers = authHeaders(rest);
        var response = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("", null), headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
