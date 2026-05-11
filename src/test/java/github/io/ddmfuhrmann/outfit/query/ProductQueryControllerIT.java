package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductDocument;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductQueryControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private Long createBrand(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Adidas"), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Calçados", null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("42"), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createColor(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest("Preto"), headers), ColorResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId, Long colorId, Long sizeId) {
        return createProduct(headers, brandId, categoryId, colorId, sizeId, "Tênis Running Pro");
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId, Long colorId, Long sizeId, String description) {
        var request = new CreateProductRequest(
                description, BigDecimal.valueOf(299.90), BigDecimal.valueOf(150.00),
                null, colorId, brandId, categoryId,
                List.of(new CreateSkuRequest("BAR-" + System.nanoTime(), sizeId, 5)));
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    @Test
    void afterProductCreated_getByIdReturnsFullDocumentWithDescriptions() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers);
        Long categoryId = createCategory(headers);
        Long sizeId = createSize(headers);
        Long colorId = createColor(headers);

        var product = createProduct(headers, brandId, categoryId, colorId, sizeId);

        var resp = rest.exchange("/catalog/products/" + product.id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), ProductDocument.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var doc = resp.getBody();
        assertThat(doc).isNotNull();
        assertThat(doc.id()).isEqualTo(product.id());
        assertThat(doc.description()).isEqualTo("Tênis Running Pro");
        assertThat(doc.active()).isTrue();
        assertThat(doc.brand()).isNotNull();
        assertThat(doc.brand().description()).isEqualTo("Adidas");
        assertThat(doc.category()).isNotNull();
        assertThat(doc.category().description()).isEqualTo("Calçados");
        assertThat(doc.color()).isNotNull();
        assertThat(doc.color().description()).isEqualTo("Preto");
        assertThat(doc.skus()).hasSize(1);
        assertThat(doc.skus().getFirst().sizeDescription()).isEqualTo("42");
    }

    @Test
    void searchByDescription_returnsMatchingDocuments() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers);
        Long categoryId = createCategory(headers);
        Long sizeId = createSize(headers);

        var product = createProduct(headers, brandId, categoryId, null, sizeId);

        var resp = rest.exchange("/catalog/products?q=Tênis+Running+Pro", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)),
                new ParameterizedTypeReference<PageResponse<ProductDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().content())
                .extracting(ProductDocument::id)
                .contains(product.id());
    }

    @Test
    void afterProductDeactivated_documentShowsActiveFalse() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers);
        Long categoryId = createCategory(headers);
        Long sizeId = createSize(headers);

        var product = createProduct(headers, brandId, categoryId, null, sizeId);

        rest.exchange("/catalog/products/" + product.id(), HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);

        var resp = rest.exchange("/catalog/products/" + product.id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), ProductDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().active()).isFalse();
    }

    @Test
    void afterSkuDeactivated_embeddedSkuShowsActiveFalse() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers);
        Long categoryId = createCategory(headers);
        Long sizeId = createSize(headers);

        var product = createProduct(headers, brandId, categoryId, null, sizeId);
        Long skuId = product.skus().getFirst().id();

        rest.exchange("/catalog/products/" + product.id() + "/skus/" + skuId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(rest)), Void.class);

        var resp = rest.exchange("/catalog/products/" + product.id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), ProductDocument.class);

        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().skus().getFirst().active()).isFalse();
    }

    @Test
    void searchByPrefix_returnsMatchingDocuments() {
        var headers = authHeaders(rest);
        Long brandId = createBrand(headers);
        Long categoryId = createCategory(headers);
        Long sizeId = createSize(headers);

        var product = createProduct(headers, brandId, categoryId, null, sizeId, "Camiseta Azul");

        var resp = rest.exchange("/catalog/products?q=Cam", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)),
                new ParameterizedTypeReference<PageResponse<ProductDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().content())
                .extracting(ProductDocument::id)
                .contains(product.id());
    }

    @Test
    void getByIdForNonExistentProductReturns404() {
        var resp = rest.exchange("/catalog/products/999999", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
