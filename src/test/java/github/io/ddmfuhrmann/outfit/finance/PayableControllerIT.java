package github.io.ddmfuhrmann.outfit.finance;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.finance.application.dto.PayPayableRequest;
import github.io.ddmfuhrmann.outfit.finance.application.dto.PayableResponse;
import github.io.ddmfuhrmann.outfit.finance.application.usecase.CancelPayablesForPurchaseUseCase;
import github.io.ddmfuhrmann.outfit.finance.domain.model.Payable;
import github.io.ddmfuhrmann.outfit.finance.domain.model.PayableStatus;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.PayableRepository;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.AddPayableRequest;
import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.purchasing.infrastructure.persistence.JpaPurchaseRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.TestCnpjFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayableControllerIT extends AbstractIT {

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
                new HttpEntity<>(new BrandRequest("Brand-PAY-" + suffix), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-PAY-" + suffix, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, String suffix) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-PAY-" + suffix), headers), SizeResponse.class);
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
                "Product PAY-" + tag, BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                purchaseDate, null, brandId, categoryId,
                List.of(new CreateSkuRequest("PAY-" + tag, sizeId, 1)));
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
                new HttpEntity<>(new AddPayableRequest(LocalDate.of(2026, Month.JULY, 1), linesTotal), headers),
                PurchaseResponse.class);
        rest.exchange("/purchases/" + purchaseId + "/confirm", HttpMethod.POST,
                new HttpEntity<>(headers), PurchaseResponse.class);

        return new ConfirmedPurchase(purchaseId, supplierId, linesTotal);
    }

    private Long findPayableByPurchaseId(Long purchaseId) {
        return payableRepository.findByPurchaseId(purchaseId).stream()
                .findFirst()
                .map(Payable::getId)
                .orElseThrow(() -> new AssertionError("No payable found for purchaseId " + purchaseId));
    }

    private PayableResponse payPayable(HttpHeaders headers, Long payableId, BigDecimal amount) {
        var resp = rest.exchange("/payables/" + payableId + "/payments", HttpMethod.POST,
                new HttpEntity<>(new PayPayableRequest(amount), headers), PayableResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @Test
    void givenConfirmedPurchase_whenPurchaseConfirmed_thenPayablesCreated() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "GCP");

        var payables = payableRepository.findByPurchaseId(p.id());

        assertThat(payables).hasSize(1);
        var payable = payables.getFirst();
        assertThat(payable.getStatus().name()).isEqualTo("OPEN");
        assertThat(payable.getAmount()).isEqualByComparingTo(p.linesTotal());
        assertThat(payable.getBalance()).isEqualByComparingTo(p.linesTotal());
    }

    @Test
    void givenOpenPayable_whenPartialPayment_thenStatusPartiallyPaid() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "OPP");
        Long payableId = findPayableByPurchaseId(p.id());
        BigDecimal partialAmount = p.linesTotal().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

        var result = payPayable(headers, payableId, partialAmount);

        assertThat(result.status()).isEqualTo("PARTIALLY_PAID");
        assertThat(result.balance()).isLessThan(p.linesTotal());
    }

    @Test
    void givenPartiallyPaidPayable_whenFinalPayment_thenStatusPaid() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "PPF");
        Long payableId = findPayableByPurchaseId(p.id());
        BigDecimal total = p.linesTotal();
        BigDecimal firstPayment = total.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = total.subtract(firstPayment);

        payPayable(headers, payableId, firstPayment);
        var result = payPayable(headers, payableId, remaining);

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void givenConfirmedPurchaseWithOpenPayable_whenCancelPayables_thenPayableCancelled() {
        // Confirmed purchases cannot be cancelled via REST (returns 422).
        // Test the finance cancel use case directly — it is what PurchaseCancelledListener delegates to.
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "CPC");
        Long payableId = findPayableByPurchaseId(p.id());

        cancelPayablesForPurchaseUseCase.execute(p.id());

        var payable = payableRepository.findById(payableId).orElseThrow();
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.CANCELLED);
    }

    @Test
    void givenPaidPayable_whenCancelPayables_thenPaidPayableNotCancelled() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "PPC");
        Long payableId = findPayableByPurchaseId(p.id());

        payPayable(headers, payableId, p.linesTotal());

        cancelPayablesForPurchaseUseCase.execute(p.id());

        var payable = payableRepository.findById(payableId).orElseThrow();
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.PAID);
    }

    @Test
    void givenOpenPayable_whenOverpayment_then400() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "OVO");
        Long payableId = findPayableByPurchaseId(p.id());

        var resp = rest.exchange("/payables/" + payableId + "/payments", HttpMethod.POST,
                new HttpEntity<>(new PayPayableRequest(new BigDecimal("99999.99")), headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenPaidPayable_whenPaymentAttempted_then422() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "PPP");
        Long payableId = findPayableByPurchaseId(p.id());

        payPayable(headers, payableId, p.linesTotal());

        var resp = rest.exchange("/payables/" + payableId + "/payments", HttpMethod.POST,
                new HttpEntity<>(new PayPayableRequest(new BigDecimal("1.00")), headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void givenCancelledPayable_whenPaymentAttempted_then422() {
        var headers = authHeaders(rest);
        var p = createConfirmedPurchase(headers, "CPT");
        Long payableId = findPayableByPurchaseId(p.id());

        cancelPayablesForPurchaseUseCase.execute(p.id());

        var resp = rest.exchange("/payables/" + payableId + "/payments", HttpMethod.POST,
                new HttpEntity<>(new PayPayableRequest(new BigDecimal("1.00")), headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
