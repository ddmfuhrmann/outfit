package github.io.ddmfuhrmann.outfit.finance;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.finance.application.dto.PayReceivableRequest;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.ReceivableRepository;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.sales.application.dto.*;
import github.io.ddmfuhrmann.outfit.sales.domain.model.CommissionStatus;
import github.io.ddmfuhrmann.outfit.sales.domain.model.PaymentModality;
import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleOrigin;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.SellerCommissionRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ActivateCommissionIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    SellerCommissionRepository sellerCommissionRepository;

    @Autowired
    ReceivableRepository receivableRepository;

    private static final AtomicInteger CPF_SEED = new AtomicInteger(1500);
    private static final LocalDate SALE_DATE = LocalDate.of(2025, Month.JUNE, 4);
    private static final LocalDate DUE_DATE = LocalDate.of(2025, Month.JULY, 4);

    private static String generateCpf() {
        int base = CPF_SEED.incrementAndGet();
        int[] d = new int[11];
        String baseStr = String.format("%09d", base);
        for (int i = 0; i < 9; i++) d[i] = baseStr.charAt(i) - '0';
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += d[i] * (10 - i);
        int r = sum % 11;
        d[9] = r < 2 ? 0 : 11 - r;
        sum = 0;
        for (int i = 0; i < 10; i++) sum += d[i] * (11 - i);
        r = sum % 11;
        d[10] = r < 2 ? 0 : 11 - r;
        StringBuilder sb = new StringBuilder();
        for (int digit : d) sb.append(digit);
        return sb.toString();
    }

    private record TestSetup(Long skuId, Long productId, Long customerId, Long salespersonId) {}

    private TestSetup setup(HttpHeaders headers) {
        long ts = System.nanoTime();
        var brandId    = createBrand(headers, ts);
        var categoryId = createCategory(headers, ts);
        var sizeId     = createSize(headers, ts);
        var product    = createProduct(headers, brandId, categoryId, sizeId, "ACT-" + ts, 50);

        Long customerId    = createParty(headers, generateCpf(), "ActCustomer " + ts, true, false, false, null);
        Long salespersonId = createParty(headers, generateCpf(), "ActSeller " + ts, false, false, true, BigDecimal.valueOf(10.0));

        return new TestSetup(product.skus().getFirst().id(), product.id(), customerId, salespersonId);
    }

    private Long createBrand(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-ACT-" + ts), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-ACT-" + ts, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-ACT-" + ts), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                          Long sizeId, String barcode, int qty) {
        var req = new CreateProductRequest(
                "Product " + barcode, BigDecimal.valueOf(200.00), BigDecimal.valueOf(100.00),
                null, null, brandId, categoryId,
                List.of(new CreateSkuRequest(barcode, sizeId, qty)));
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(req, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private Long createParty(HttpHeaders headers, String cpf, String name,
                              boolean customer, boolean supplier, boolean salesperson,
                              BigDecimal commissionPercent) {
        var req = new CreatePartyRequest(
                PersonType.INDIVIDUAL, null, cpf,
                name, name,
                customer, supplier, salesperson,
                commissionPercent,
                null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, headers), PartyCreatedResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    /** Creates a sale with a single INSTALLMENT modality installment (deferred). */
    private SaleResponse createSaleWithDeferredInstallment(HttpHeaders headers, TestSetup s) {
        BigDecimal total = BigDecimal.valueOf(200.00);
        var req = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                SALE_DATE,
                null,
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 2, BigDecimal.valueOf(100.00))),
                List.of(new CreateSaleInstallmentRequest(PaymentModality.INSTALLMENT, DUE_DATE, total)),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));
        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(req, headers), SaleResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private Long findReceivableId(Long saleId) {
        return receivableRepository.findAll().stream()
                .filter(r -> r.getSaleId().equals(saleId))
                .map(r -> r.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No receivable found for saleId " + saleId));
    }

    @Test
    void givenSaleWithDeferredInstallmentAndCommission_whenReceivablePaid_thenCommissionPendingAmountReduced() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        var sale = createSaleWithDeferredInstallment(headers, s);
        Long receivableId = findReceivableId(sale.id());

        // Verify initial commission state — INSTALLMENT is 100% deferred → status should be PARTIAL
        // (earnedAmount = base * commissionPercent, pendingAmount = deferred * commissionPercent > 0)
        var commissionsBeforePayment = sellerCommissionRepository.findBySaleId(sale.id());
        assertThat(commissionsBeforePayment).hasSize(1);
        var commissionBefore = commissionsBeforePayment.getFirst();
        BigDecimal pendingBefore = commissionBefore.getPendingAmount();
        assertThat(pendingBefore).isGreaterThan(BigDecimal.ZERO);

        // Pay partial receivable — triggers ReceivablePaid (only when fully paid), so pay in full
        rest.exchange("/receivables/" + receivableId + "/payments", HttpMethod.POST,
                new HttpEntity<>(new PayReceivableRequest(new BigDecimal("200.00")), headers), String.class);

        // Verify commission was updated
        var commissionsAfterPayment = sellerCommissionRepository.findBySaleId(sale.id());
        assertThat(commissionsAfterPayment).hasSize(1);
        var commissionAfter = commissionsAfterPayment.getFirst();
        assertThat(commissionAfter.getPendingAmount()).isLessThan(pendingBefore);
    }

    @Test
    void givenSaleWithSingleDeferredInstallment_whenReceivablePaid_thenCommissionStatusEarned() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        var sale = createSaleWithDeferredInstallment(headers, s);
        Long receivableId = findReceivableId(sale.id());

        // Pay the receivable in full — ReceivablePaid event fires → ActivateCommissionUseCase
        rest.exchange("/receivables/" + receivableId + "/payments", HttpMethod.POST,
                new HttpEntity<>(new PayReceivableRequest(new BigDecimal("200.00")), headers), String.class);

        var commissions = sellerCommissionRepository.findBySaleId(sale.id());
        assertThat(commissions).hasSize(1);
        var commission = commissions.getFirst();
        // Single deferred installment paid in full → all pending unlocked → status EARNED
        assertThat(commission.getStatus()).isEqualTo(CommissionStatus.EARNED);
        assertThat(commission.getPendingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
