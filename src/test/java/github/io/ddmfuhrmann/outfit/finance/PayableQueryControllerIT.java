package github.io.ddmfuhrmann.outfit.finance;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.finance.application.dto.PayPayableRequest;
import github.io.ddmfuhrmann.outfit.finance.application.usecase.CancelPayablesForPurchaseUseCase;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.PayableRepository;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.AddPayableRequest;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.purchasing.infrastructure.persistence.JpaPurchaseRepository;
import github.io.ddmfuhrmann.outfit.query.application.dto.PayableDocument;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.TestCnpjFactory;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayableQueryControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JpaPurchaseRepository purchaseRepository;

    @Autowired
    PayableRepository payableRepository;

    @Autowired
    CancelPayablesForPurchaseUseCase cancelPayablesForPurchaseUseCase;

    private Long createBrand(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-PQC-" + suffix), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-PQC-" + suffix, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-PQC-" + suffix), headers), SizeResponse.class);
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

    private record ConfirmedPurchase(Long id, Long supplierId, BigDecimal linesTotal) {}

    private ConfirmedPurchase createConfirmedPurchase(HttpHeaders headers, String suffix) {
        long ts = System.nanoTime();
        String tag = suffix + "-" + ts;
        var supplierId = createSupplierParty(headers);
        var brandId = createBrand(headers, tag);
        addSupplierToBrand(headers, brandId, supplierId);
        var categoryId = createCategory(headers, tag);
        var sizeId = createSize(headers, tag);
        LocalDate purchaseDate = LocalDate.of(2025, Month.JUNE, 1);

        var productReq = new CreateProductRequest(
                "Product PQC-" + tag, BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                purchaseDate, null, brandId, categoryId,
                List.of(new CreateSkuRequest("PQC-" + tag, sizeId, 1)));
        rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(productReq, headers), ProductResponse.class);

        var purchase = purchaseRepository.findWithLinesByBrandIdAndPurchaseDateAndStatus(
                brandId, purchaseDate, PurchaseStatus.OPEN);
        assertThat(purchase).isPresent();

        BigDecimal linesTotal = purchase.get().getLines().stream()
                .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long purchaseId = purchase.get().getId();

        rest.exchange("/purchases/" + purchaseId + "/payables", HttpMethod.POST,
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.SEPTEMBER, 1), linesTotal), headers),
                PurchaseResponse.class);
        rest.exchange("/purchases/" + purchaseId + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        return new ConfirmedPurchase(purchaseId, supplierId, linesTotal);
    }

    private Long findPayableId(Long purchaseId) {
        return payableRepository.findByPurchaseId(purchaseId).stream()
                .findFirst()
                .map(p -> p.getId())
                .orElseThrow(() -> new AssertionError("No payable found for purchaseId " + purchaseId));
    }

    @Test
    void givenPayable_whenGetById_thenReturnsDocument() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "GBI");
        Long payableId = findPayableId(p.id());

        var resp = rest.exchange("/payables/" + payableId, HttpMethod.GET,
                new HttpEntity<>(headers), PayableDocument.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var doc = resp.getBody();
        assertThat(doc.payableId()).isEqualTo(payableId);
        assertThat(doc.purchaseId()).isEqualTo(p.id());
        assertThat(doc.supplierId()).isEqualTo(p.supplierId());
        assertThat(doc.status()).isEqualTo("OPEN");
        assertThat(doc.amount()).isEqualByComparingTo(p.linesTotal());
        assertThat(doc.balance()).isEqualByComparingTo(p.linesTotal());
    }

    @Test
    void givenMultiplePayables_whenFilterBySupplierId_thenReturnsFiltered() {
        var headers = authHeaders(rest);
        var p1 = createConfirmedPurchase(headers, "FSI1");
        var p2 = createConfirmedPurchase(headers, "FSI2");

        var resp = rest.exchange("/payables?supplierId=" + p1.supplierId() + "&size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<PayableDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = resp.getBody().content();
        assertThat(content).isNotEmpty();
        assertThat(content).allMatch(d -> d.supplierId().equals(p1.supplierId()));
        assertThat(content).noneMatch(d -> d.supplierId().equals(p2.supplierId()));
    }

    @Test
    void givenCancelledPayable_whenFilterByStatus_thenReturnsCancelled() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "FCS");
        Long payableId = findPayableId(p.id());

        // Cancel all open payables for this purchase via the use case (publishes PayableCancelled event)
        cancelPayablesForPurchaseUseCase.execute(p.id());

        var resp = rest.exchange("/payables?supplierId=" + p.supplierId() + "&status=CANCELLED&size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<PayableDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).anyMatch(d -> d.payableId().equals(payableId)
                && "CANCELLED".equals(d.status()));
    }
}
