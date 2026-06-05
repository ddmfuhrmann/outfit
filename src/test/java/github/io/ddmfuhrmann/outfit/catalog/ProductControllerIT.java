package github.io.ddmfuhrmann.outfit.catalog;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.ProductSkuCreated;
import github.io.ddmfuhrmann.outfit.catalog.domain.model.Product;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    // --- helpers ---

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

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                          Long sizeId, String barcode) {
        var request = new CreateProductRequest(
                "Test Product", BigDecimal.valueOf(99.90), BigDecimal.valueOf(50.00),
                null, null, brandId, categoryId,
                List.of(new CreateSkuRequest(barcode, sizeId, 10))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private Long createSupplierParty(HttpHeaders headers, String cnpj) {
        var req = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, cnpj, null,
                "Fornecedora Produto S.A.", "Fornecedora Produto",
                false, true, false,
                null, null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, headers), PartyCreatedResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    // --- tests ---

    @Test
    void createProductReturns201() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-Create");
        var categoryId = createCategory(headers, "Category-Create");
        var sizeId = createSize(headers, "Size-Create");

        var request = new CreateProductRequest(
                "Shirt XL", BigDecimal.valueOf(120.00), BigDecimal.valueOf(60.00),
                null, null, brandId, categoryId,
                List.of(
                        new CreateSkuRequest("BC-C-001", sizeId, 5),
                        new CreateSkuRequest("BC-C-002", sizeId, 3)
                )
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().description()).isEqualTo("Shirt XL");
        assertThat(resp.getBody().skus()).hasSize(2);
        assertThat(resp.getBody().active()).isTrue();
    }

    @Test
    void updateProductReturns200() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-Update");
        var categoryId = createCategory(headers, "Category-Update");
        var sizeId = createSize(headers, "Size-Update");
        var product = createProduct(headers, brandId, categoryId, sizeId, "BC-U-001");

        var request = new UpdateProductRequest(
                "Updated Shirt", BigDecimal.valueOf(130.00), BigDecimal.valueOf(65.00),
                null, null, brandId, categoryId
        );
        var resp = rest.exchange("/catalog/products/" + product.id(), HttpMethod.PUT,
                new HttpEntity<>(request, headers), ProductResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().description()).isEqualTo("Updated Shirt");
        assertThat(resp.getBody().price()).isEqualByComparingTo(BigDecimal.valueOf(130.00));
    }

    @Test
    void addSkuReturns201() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-AddSku");
        var categoryId = createCategory(headers, "Category-AddSku");
        var sizeId = createSize(headers, "Size-AddSku");
        var product = createProduct(headers, brandId, categoryId, sizeId, "BC-AS-001");

        var request = new AddSkuRequest("BC-AS-002", sizeId, 7);
        var resp = rest.exchange("/catalog/products/" + product.id() + "/skus", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductSkuResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().barcode()).isEqualTo("BC-AS-002");
        assertThat(resp.getBody().sizeDescription()).isEqualTo("Size-AddSku");
        assertThat(resp.getBody().active()).isTrue();
    }

    @Test
    void deactivateSkuReturns204() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-DeactSku");
        var categoryId = createCategory(headers, "Category-DeactSku");
        var sizeId = createSize(headers, "Size-DeactSku");
        var product = createProduct(headers, brandId, categoryId, sizeId, "BC-DS-001");
        var skuId = product.skus().getFirst().id();

        var resp = rest.exchange("/catalog/products/" + product.id() + "/skus/" + skuId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deactivateProductReturns204() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-DeactProd");
        var categoryId = createCategory(headers, "Category-DeactProd");
        var sizeId = createSize(headers, "Size-DeactProd");
        var product = createProduct(headers, brandId, categoryId, sizeId, "BC-DP-001");

        var resp = rest.exchange("/catalog/products/" + product.id(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deactivateAlreadyInactiveProductReturns422() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-422");
        var categoryId = createCategory(headers, "Category-422");
        var sizeId = createSize(headers, "Size-422");
        var product = createProduct(headers, brandId, categoryId, sizeId, "BC-422-001");

        rest.exchange("/catalog/products/" + product.id(), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        var resp = rest.exchange("/catalog/products/" + product.id(),
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createProductBlankBarcodeReturns400() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-BlankBC");
        var categoryId = createCategory(headers, "Category-BlankBC");
        var sizeId = createSize(headers, "Size-BlankBC");

        var request = new CreateProductRequest(
                "Shirt", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                null, null, brandId, categoryId,
                List.of(new CreateSkuRequest("", sizeId, 1))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createProductMissingBrandIdReturns400() {
        var headers = authHeaders(rest);
        var categoryId = createCategory(headers, "Category-NoBrand");
        var sizeId = createSize(headers, "Size-NoBrand");

        var request = new CreateProductRequest(
                "Shirt", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                null, null, null, categoryId,
                List.of(new CreateSkuRequest("BC-NB-001", sizeId, 1))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createProductMissingCategoryIdReturns400() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-NoCat");
        var sizeId = createSize(headers, "Size-NoCat");

        var request = new CreateProductRequest(
                "Shirt", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                null, null, brandId, null,
                List.of(new CreateSkuRequest("BC-NC-001", sizeId, 1))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createProductDuplicateBarcodeReturns409() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-DupBC");
        var categoryId = createCategory(headers, "Category-DupBC");
        var sizeId = createSize(headers, "Size-DupBC");

        createProduct(headers, brandId, categoryId, sizeId, "BC-DUP-001");

        var request = new CreateProductRequest(
                "Another Shirt", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                null, null, brandId, categoryId,
                List.of(new CreateSkuRequest("BC-DUP-001", sizeId, 1))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteColorInUseReturns422() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers, "Brand-ColorGuard");
        var categoryId = createCategory(headers, "Category-ColorGuard");
        var sizeId = createSize(headers, "Size-ColorGuard");
        var colorId = createColor(headers, "Color-ColorGuard");

        var request = new CreateProductRequest(
                "Colored Shirt", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                null, colorId, brandId, categoryId,
                List.of(new CreateSkuRequest("BC-CG-001", sizeId, 1))
        );
        rest.exchange("/catalog/products", HttpMethod.POST, new HttpEntity<>(request, headers), ProductResponse.class);

        var resp = rest.exchange("/catalog/colors/" + colorId, HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createProduct_skuCreatedEventCarriesSupplierIds() {
        var supplierIds = List.of(101L, 202L);
        var product = Product.builder()
                .description("Shirt With Suppliers")
                .price(BigDecimal.valueOf(100.00))
                .cost(BigDecimal.valueOf(50.00))
                .brandId(1L)
                .categoryId(1L)
                .build(supplierIds);

        product.addSku("BC-SUP-001", 1L, 5, supplierIds);

        var skuCreatedEvent = (ProductSkuCreated) product.getRegisteredEvents().stream()
                .filter(e -> e instanceof ProductSkuCreated)
                .findFirst()
                .orElseThrow();

        assertThat(skuCreatedEvent.snapshot().supplierIds()).containsExactlyInAnyOrderElementsOf(supplierIds);
    }

    @Test
    void createProduct_skuCreatedEventCarriesEmptySupplierIds_whenBrandHasNoSupplier() {
        var product = Product.builder()
                .description("Shirt No Supplier")
                .price(BigDecimal.valueOf(100.00))
                .cost(BigDecimal.valueOf(50.00))
                .brandId(1L)
                .categoryId(1L)
                .build(List.of());

        product.addSku("BC-NOSUP-001", 1L, 5, List.of());

        var skuCreatedEvent = (ProductSkuCreated) product.getRegisteredEvents().stream()
                .filter(e -> e instanceof ProductSkuCreated)
                .findFirst()
                .orElseThrow();

        assertThat(skuCreatedEvent.snapshot().supplierIds()).isEmpty();
    }

}
