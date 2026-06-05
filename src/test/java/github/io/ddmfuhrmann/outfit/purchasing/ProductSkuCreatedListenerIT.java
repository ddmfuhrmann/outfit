package github.io.ddmfuhrmann.outfit.purchasing;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.purchasing.infrastructure.persistence.JpaPurchaseRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.TestCnpjFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSkuCreatedListenerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JpaPurchaseRepository purchaseRepository;

    private Long createBrand(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-PR-" + suffix), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-PR-" + suffix, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-PR-" + suffix), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                          Long sizeId, String barcode, LocalDate purchaseDate) {
        var request = new CreateProductRequest(
                "Product " + barcode, BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                purchaseDate, null, brandId, categoryId,
                java.util.List.of(new CreateSkuRequest(barcode, sizeId, 1))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private Long createSupplierParty(HttpHeaders headers) {
        String cnpj = TestCnpjFactory.generate();
        var req = new CreatePartyRequest(
                PersonType.LEGAL_ENTITY, cnpj, null,
                "Fornecedora " + cnpj + " S.A.", "Fornecedora " + cnpj,
                false, true, false,
                null, null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, headers), PartyCreatedResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private void addSupplierToBrand(HttpHeaders headers, Long brandId, Long supplierId) {
        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.POST, new HttpEntity<>(headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- tests ---

    @Test
    void skuWithNullPurchaseDate_noPurchaseCreated() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers, "NullDate-" + ts);
        var categoryId = createCategory(headers, "NullDate-" + ts);
        var sizeId = createSize(headers, "NullDate-" + ts);

        createProduct(headers, brandId, categoryId, sizeId, "PR-ND-" + ts, null);

        var result = purchaseRepository.findByBrandIdAndPurchaseDateAndStatus(
                brandId, LocalDate.of(2025, Month.JANUARY, 10), PurchaseStatus.OPEN);
        assertThat(result).isEmpty();

        // Also verify no purchase at all was created for this brand
        var all = purchaseRepository.findAll().stream()
                .filter(p -> p.getBrandId().equals(brandId))
                .toList();
        assertThat(all).isEmpty();
    }

    @Test
    void firstSkuWithPurchaseDate_createsNewPurchaseWithOneLine() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers, "First-" + ts);
        var categoryId = createCategory(headers, "First-" + ts);
        var sizeId = createSize(headers, "First-" + ts);
        var purchaseDate = LocalDate.of(2025, Month.JANUARY, 10);

        createProduct(headers, brandId, categoryId, sizeId, "PR-F-" + ts, purchaseDate);

        var purchase = purchaseRepository.findWithLinesByBrandIdAndPurchaseDateAndStatus(
                brandId, purchaseDate, PurchaseStatus.OPEN);
        assertThat(purchase).isPresent();
        assertThat(purchase.get().getBrandId()).isEqualTo(brandId);
        assertThat(purchase.get().getPurchaseDate()).isEqualTo(purchaseDate);
        assertThat(purchase.get().getStatus()).isEqualTo(PurchaseStatus.OPEN);
        assertThat(purchase.get().getLines()).hasSize(1);
    }

    @Test
    void secondSkuSameBrandAndDate_addsLineToExistingPurchase() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers, "Second-" + ts);
        var categoryId = createCategory(headers, "Second-" + ts);
        var sizeId = createSize(headers, "Second-" + ts);
        var purchaseDate = LocalDate.of(2025, Month.FEBRUARY, 15);

        createProduct(headers, brandId, categoryId, sizeId, "PR-S1-" + ts, purchaseDate);
        createProduct(headers, brandId, categoryId, sizeId, "PR-S2-" + ts, purchaseDate);

        var purchases = purchaseRepository.findAllWithLinesByBrandId(brandId);
        assertThat(purchases).hasSize(1);
        assertThat(purchases.getFirst().getLines()).hasSize(2);
    }

    @Test
    void skuWithOneSupplierOnBrand_supplierIdSet() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var supplierId = createSupplierParty(headers);
        var brandId = createBrand(headers, "OneSupp-" + ts);
        addSupplierToBrand(headers, brandId, supplierId);
        var categoryId = createCategory(headers, "OneSupp-" + ts);
        var sizeId = createSize(headers, "OneSupp-" + ts);
        var purchaseDate = LocalDate.of(2025, Month.MARCH, 1);

        createProduct(headers, brandId, categoryId, sizeId, "PR-OS-" + ts, purchaseDate);

        var purchase = purchaseRepository.findByBrandIdAndPurchaseDateAndStatus(
                brandId, purchaseDate, PurchaseStatus.OPEN);
        assertThat(purchase).isPresent();
        assertThat(purchase.get().getSupplierId()).isEqualTo(supplierId);
    }

    @Test
    void skuWithMultipleSuppliersOnBrand_supplierIdNull() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var supplierId1 = createSupplierParty(headers);
        var supplierId2 = createSupplierParty(headers);
        var brandId = createBrand(headers, "MultiSupp-" + ts);
        addSupplierToBrand(headers, brandId, supplierId1);
        addSupplierToBrand(headers, brandId, supplierId2);
        var categoryId = createCategory(headers, "MultiSupp-" + ts);
        var sizeId = createSize(headers, "MultiSupp-" + ts);
        var purchaseDate = LocalDate.of(2025, Month.APRIL, 1);

        createProduct(headers, brandId, categoryId, sizeId, "PR-MS-" + ts, purchaseDate);

        var purchase = purchaseRepository.findByBrandIdAndPurchaseDateAndStatus(
                brandId, purchaseDate, PurchaseStatus.OPEN);
        assertThat(purchase).isPresent();
        assertThat(purchase.get().getSupplierId()).isNull();
    }

    @Test
    void skuWithNoSuppliersOnBrand_supplierIdNull() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers, "NoSupp-" + ts);
        var categoryId = createCategory(headers, "NoSupp-" + ts);
        var sizeId = createSize(headers, "NoSupp-" + ts);
        var purchaseDate = LocalDate.of(2025, Month.MAY, 1);

        createProduct(headers, brandId, categoryId, sizeId, "PR-NS-" + ts, purchaseDate);

        var purchase = purchaseRepository.findByBrandIdAndPurchaseDateAndStatus(
                brandId, purchaseDate, PurchaseStatus.OPEN);
        assertThat(purchase).isPresent();
        assertThat(purchase.get().getSupplierId()).isNull();
    }
}
