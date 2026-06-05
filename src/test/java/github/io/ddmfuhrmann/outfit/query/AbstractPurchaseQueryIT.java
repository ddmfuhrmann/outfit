package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.purchasing.infrastructure.persistence.JpaPurchaseRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.TestCnpjFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractPurchaseQueryIT extends AbstractIT {

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JpaPurchaseRepository purchaseRepository;

    protected record PurchaseSetup(Long purchaseId, Long brandId, Long supplierId, LocalDate purchaseDate) {}

    protected Long createBrand(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-PQ-" + suffix), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    protected Long createCategory(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-PQ-" + suffix, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    protected Long createSize(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-PQ-" + suffix), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    protected Long createSupplierParty(HttpHeaders headers) {
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

    protected void addSupplierToBrand(HttpHeaders headers, Long brandId, Long supplierId) {
        var resp = rest.exchange("/catalog/brands/" + brandId + "/suppliers/" + supplierId,
                HttpMethod.POST, new HttpEntity<>(headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    protected ProductResponse createProductWithSku(HttpHeaders headers, Long brandId, Long categoryId,
                                                    Long sizeId, String barcode, LocalDate purchaseDate) {
        var request = new CreateProductRequest(
                "Product " + barcode, BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                purchaseDate, null, brandId, categoryId,
                List.of(new CreateSkuRequest(barcode, sizeId, 1)));
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    protected PurchaseSetup setupPurchase(HttpHeaders headers, String suffix, LocalDate purchaseDate) {
        long ts = System.nanoTime();
        String tag = suffix + "-" + ts;
        var supplierId = createSupplierParty(headers);
        var brandId = createBrand(headers, tag);
        addSupplierToBrand(headers, brandId, supplierId);
        var categoryId = createCategory(headers, tag);
        var sizeId = createSize(headers, tag);
        createProductWithSku(headers, brandId, categoryId, sizeId, "PQ-" + tag, purchaseDate);
        var purchase = purchaseRepository.findByBrandIdAndPurchaseDateAndStatus(
                brandId, purchaseDate, PurchaseStatus.OPEN);
        assertThat(purchase).isPresent();
        return new PurchaseSetup(purchase.get().getId(), brandId, supplierId, purchaseDate);
    }
}
