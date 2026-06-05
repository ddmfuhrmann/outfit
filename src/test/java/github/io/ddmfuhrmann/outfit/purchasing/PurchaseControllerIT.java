package github.io.ddmfuhrmann.outfit.purchasing;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.AddPayableRequest;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.UpdateObservationsRequest;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JpaPurchaseRepository purchaseRepository;

    private Long createBrand(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-PC-" + suffix), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-PC-" + suffix, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-PC-" + suffix), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
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

    private ProductResponse createProductWithSku(HttpHeaders headers, Long brandId, Long categoryId,
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

    private record OpenPurchase(Long id, Long brandId, LocalDate purchaseDate, BigDecimal linesTotal) {}

    private OpenPurchase createOpenPurchase(HttpHeaders headers, String suffix) {
        long ts = System.nanoTime();
        String tag = suffix + "-" + ts;
        var supplierId = createSupplierParty(headers);
        var brandId = createBrand(headers, tag);
        addSupplierToBrand(headers, brandId, supplierId);
        var categoryId = createCategory(headers, tag);
        var sizeId = createSize(headers, tag);
        var purchaseDate = LocalDate.of(2025, Month.JUNE, 1);
        createProductWithSku(headers, brandId, categoryId, sizeId, "PC-" + tag, purchaseDate);
        var purchase = purchaseRepository.findWithLinesByBrandIdAndPurchaseDateAndStatus(
                brandId, purchaseDate, PurchaseStatus.OPEN);
        assertThat(purchase).isPresent();
        BigDecimal linesTotal = purchase.get().getLines().stream()
                .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OpenPurchase(purchase.get().getId(), brandId, purchaseDate, linesTotal);
    }

    @Test
    void addPayable_onOpenPurchase_returns200WithUpdatedPayables() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "APO");

        var req = new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), BigDecimal.valueOf(50.00));
        var resp = rest.exchange("/purchases/" + p.id() + "/payables", HttpMethod.POST,
                new HttpEntity<>(req, headers), PurchaseResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().payables()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(resp.getBody().payables().getFirst().id()).isNotNull();
    }

    @Test
    void addPayable_onNonOpenPurchase_returns422() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "APNon");
        var total = p.linesTotal();

        rest.exchange("/purchases/" + p.id() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), total), headers),
                PurchaseResponse.class);
        rest.exchange("/purchases/" + p.id() + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        var resp = rest.exchange("/purchases/" + p.id() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.AUGUST, 1), BigDecimal.valueOf(10.00)), headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void addPayable_withUnknownId_returns404() {
        var headers = authHeaders(rest);
        var req = new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), BigDecimal.valueOf(50.00));
        var resp = rest.exchange("/purchases/99999999/payables", HttpMethod.POST,
                new HttpEntity<>(req, headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removePayable_onOpenPurchase_returns204() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "RPO");

        var addResp = rest.exchange("/purchases/" + p.id() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), BigDecimal.valueOf(50.00)), headers),
                PurchaseResponse.class);
        assertThat(addResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Long payableId = addResp.getBody().payables().getFirst().id();

        var resp = rest.exchange("/purchases/" + p.id() + "/payables/" + payableId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void removePayable_withUnknownPayableId_returns400() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "RPUnk");

        var resp = rest.exchange("/purchases/" + p.id() + "/payables/99999999",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void confirm_withValidPurchase_returns200WithConfirmedStatus() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "CVP");
        var total = p.linesTotal();

        rest.exchange("/purchases/" + p.id() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), total), headers),
                PurchaseResponse.class);

        var resp = rest.exchange("/purchases/" + p.id() + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo("CONFIRMED");
    }

    @Test
    void confirm_withNoPayables_returns422() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "CNP");

        var resp = rest.exchange("/purchases/" + p.id() + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void confirm_withUnbalancedPayables_returns422() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "CUP");

        rest.exchange("/purchases/" + p.id() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), BigDecimal.valueOf(999.99)), headers),
                PurchaseResponse.class);

        var resp = rest.exchange("/purchases/" + p.id() + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void cancel_onOpenPurchase_returns200WithCancelledStatus() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "COP");

        var resp = rest.exchange("/purchases/" + p.id() + "/cancel", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_onConfirmedPurchase_returns422() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "CCP");
        var total = p.linesTotal();

        rest.exchange("/purchases/" + p.id() + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), total), headers),
                PurchaseResponse.class);
        rest.exchange("/purchases/" + p.id() + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        var resp = rest.exchange("/purchases/" + p.id() + "/cancel", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateObservations_onOpenPurchase_returns200WithUpdatedObservations() {
        var headers = authHeaders(rest);
        var p = createOpenPurchase(headers, "UOO");

        var req = new UpdateObservationsRequest("new notes");
        var resp = rest.exchange("/purchases/" + p.id() + "/observations", HttpMethod.PUT,
                new HttpEntity<>(req, headers), PurchaseResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().observations()).isEqualTo("new notes");
    }
}
