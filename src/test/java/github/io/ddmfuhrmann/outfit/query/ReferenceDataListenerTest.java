package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductDocument;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDataListenerTest extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private Long createBrand(HttpHeaders headers, String description) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest(description), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, String description) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest(description, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, String description) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest(description), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createColor(HttpHeaders headers, String description) {
        var resp = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest(description), headers), ColorResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createProductWithColor(HttpHeaders headers, Long brandId, Long categoryId,
                                        Long colorId, Long sizeId) {
        var request = new CreateProductRequest(
                "Product " + System.nanoTime(), BigDecimal.valueOf(100.00), BigDecimal.valueOf(60.00),
                null, colorId, brandId, categoryId,
                List.of(new CreateSkuRequest("SKU-" + System.nanoTime(), sizeId, 1)));
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    @Test
    void whenColorRenamed_productDocumentUpdatesColorDescription() {
        var headers = authHeaders(rest);
        Long colorId = createColor(headers, "Azul");
        Long brandId = createBrand(headers, "Nike");
        Long categoryId = createCategory(headers, "Roupas");
        Long sizeId = createSize(headers, "M");

        Long productId = createProductWithColor(headers, brandId, categoryId, colorId, sizeId);

        rest.exchange("/catalog/colors/" + colorId, HttpMethod.PUT,
                new HttpEntity<>(new ColorRequest("Azul Marinho"), headers), ColorResponse.class);

        var resp = rest.exchange("/catalog/products/" + productId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), ProductDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().color()).isNotNull();
        assertThat(resp.getBody().color().description()).isEqualTo("Azul Marinho");
    }

    @Test
    void whenBrandRenamed_productDocumentUpdatesBrandDescription() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Puma Original");
        Long categoryId = createCategory(headers, "Esporte");
        Long sizeId = createSize(headers, "G");

        Long productId = createProductWithColor(headers, brandId, categoryId, null, sizeId);

        rest.exchange("/catalog/brands/" + brandId, HttpMethod.PUT,
                new HttpEntity<>(new BrandRequest("Puma Sport"), headers), BrandResponse.class);

        var resp = rest.exchange("/catalog/products/" + productId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), ProductDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().brand().description()).isEqualTo("Puma Sport");
    }

    @Test
    void whenCategoryRenamed_productDocumentUpdatesCategoryDescription() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Fila");
        Long categoryId = createCategory(headers, "Moda");
        Long sizeId = createSize(headers, "P");

        Long productId = createProductWithColor(headers, brandId, categoryId, null, sizeId);

        rest.exchange("/catalog/categories/" + categoryId, HttpMethod.PUT,
                new HttpEntity<>(new CategoryRequest("Moda Feminina", null), headers), CategoryResponse.class);

        var resp = rest.exchange("/catalog/products/" + productId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), ProductDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().category().description()).isEqualTo("Moda Feminina");
    }

    @Test
    void whenSizeRenamed_skuSizeDescriptionUpdatedInProductDocument() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers, "Lacoste");
        Long categoryId = createCategory(headers, "Casual");
        Long sizeId = createSize(headers, "38");

        Long productId = createProductWithColor(headers, brandId, categoryId, null, sizeId);

        rest.exchange("/catalog/sizes/" + sizeId, HttpMethod.PUT,
                new HttpEntity<>(new SizeRequest("38 EU"), headers), SizeResponse.class);

        var resp = rest.exchange("/catalog/products/" + productId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), ProductDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().skus()).hasSize(1);
        assertThat(resp.getBody().skus().getFirst().sizeDescription()).isEqualTo("38 EU");
    }
}
