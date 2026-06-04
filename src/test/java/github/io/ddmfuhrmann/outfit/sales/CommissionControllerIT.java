package github.io.ddmfuhrmann.outfit.sales;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.sales.application.dto.*;
import github.io.ddmfuhrmann.outfit.sales.domain.model.CommissionStatus;
import github.io.ddmfuhrmann.outfit.sales.domain.model.PaymentModality;
import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleOrigin;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CommissionControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private static final AtomicInteger CPF_SEED = new AtomicInteger(900);

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

    private record TestSetup(Long skuId, Long productId, Long customerId, Long salespersonId,
                             Long salespersonId2) {}

    private TestSetup setup(HttpHeaders headers) {
        long ts = System.nanoTime();
        var brandId    = createBrand(headers, ts);
        var categoryId = createCategory(headers, ts);
        var sizeId     = createSize(headers, ts);
        var product    = createProduct(headers, brandId, categoryId, sizeId, "COM-" + ts, 50);

        Long customerId     = createParty(headers, generateCpf(), "Customer " + ts, true, false, false, null);
        Long salespersonId  = createParty(headers, generateCpf(), "Seller1 " + ts, false, false, true, BigDecimal.valueOf(5.0));
        Long salespersonId2 = createParty(headers, generateCpf(), "Seller2 " + ts, false, false, true, BigDecimal.valueOf(5.0));

        return new TestSetup(product.skus().getFirst().id(), product.id(), customerId, salespersonId, salespersonId2);
    }

    private Long createBrand(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-COM-" + ts), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-COM-" + ts, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-COM-" + ts), headers), SizeResponse.class);
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

    private SaleResponse createDirectSale(HttpHeaders headers, TestSetup s,
                                           BigDecimal unitPrice, int qty,
                                           PaymentModality modality) {
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        var req = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), qty, unitPrice)),
                List.of(new CreateSaleInstallmentRequest(modality, LocalDate.now().plusDays(30), total)),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));
        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(req, headers), SaleResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private PageResponse<SellerCommissionResponse> listCommissions(HttpHeaders headers,
                                                                    Long salespersonId,
                                                                    CommissionStatus status) {
        String url = "/commissions?size=100";
        if (salespersonId != null) url += "&salespersonId=" + salespersonId;
        if (status != null) url += "&status=" + status.name();
        var resp = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<SellerCommissionResponse>>() {});
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @Test
    void createSaleCreatesCommissionsForSellers() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        var sale = createDirectSale(headers, s, BigDecimal.valueOf(200.00), 2, PaymentModality.PIX);

        var page = listCommissions(headers, s.salespersonId(), null);

        assertThat(page.content()).anyMatch(c -> c.saleId().equals(sale.id())
                && c.salespersonId().equals(s.salespersonId()));
    }

    @Test
    void createSaleWithBonusTierAddsBonus() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        // Create bonus tier: netAmount in [100, 999] → 2% bonus (range avoids overlap with other test tiers)
        var tierReq = new CreateCommissionBonusTierRequest(
                BigDecimal.valueOf(100), BigDecimal.valueOf(999), BigDecimal.valueOf(2.0));
        var tierResp = rest.exchange("/commission-bonus-tiers", HttpMethod.POST,
                new HttpEntity<>(tierReq, headers), CommissionBonusTierResponse.class);
        assertThat(tierResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Sale netAmount = 500 → within tier range
        var sale = createDirectSale(headers, s, BigDecimal.valueOf(250.00), 2, PaymentModality.PIX);

        var page = listCommissions(headers, s.salespersonId(), null);
        var commission = page.content().stream()
                .filter(c -> c.saleId().equals(sale.id()))
                .findFirst();
        assertThat(commission).isPresent();
        assertThat(commission.get().bonusAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void listCommissionsBySellerIdFilters() {
        var headers = authHeaders(rest);
        var s1 = setup(headers);
        var s2 = setup(headers);

        // Create a dedicated sale for each seller (each in their own setup with unique items)
        createDirectSale(headers, s1, BigDecimal.valueOf(100.00), 1, PaymentModality.PIX);
        createDirectSale(headers, s2, BigDecimal.valueOf(100.00), 1, PaymentModality.PIX);

        var page = listCommissions(headers, s1.salespersonId(), null);
        assertThat(page.content()).allMatch(c -> c.salespersonId().equals(s1.salespersonId()));
        assertThat(page.content()).noneMatch(c -> c.salespersonId().equals(s2.salespersonId()));
    }

    @Test
    void listCommissionsByStatusFilters() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        // INSTALLMENT is deferred → commissionBase = deferred * 0.7, earnedAmount > 0, pendingAmount > 0 → PARTIAL
        createDirectSale(headers, s, BigDecimal.valueOf(200.00), 2, PaymentModality.INSTALLMENT);

        var partialPage = listCommissions(headers, s.salespersonId(), CommissionStatus.PARTIAL);
        assertThat(partialPage.content()).anyMatch(c -> c.salespersonId().equals(s.salespersonId()));
    }

    @Test
    void createCommissionBonusTierReturns201() {
        var headers = authHeaders(rest);

        var req = new CreateCommissionBonusTierRequest(
                BigDecimal.valueOf(5000), BigDecimal.valueOf(20000), BigDecimal.valueOf(3.0));
        var resp = rest.exchange("/commission-bonus-tiers", HttpMethod.POST,
                new HttpEntity<>(req, headers), CommissionBonusTierResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).isNotNull();
        var body = resp.getBody();
        assertThat(body.id()).isNotNull();
        assertThat(body.minAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(body.maxAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(body.bonusPercent()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(body.active()).isTrue();
    }

    @Test
    void listCommissionBonusTiersReturnsAll() {
        var headers = authHeaders(rest);

        var req = new CreateCommissionBonusTierRequest(
                BigDecimal.valueOf(50000), BigDecimal.valueOf(100000), BigDecimal.valueOf(1.5));
        rest.exchange("/commission-bonus-tiers", HttpMethod.POST,
                new HttpEntity<>(req, headers), CommissionBonusTierResponse.class);

        var resp = rest.exchange("/commission-bonus-tiers", HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<CommissionBonusTierResponse>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotEmpty();
    }

    @Test
    void deactivateCommissionBonusTierReturns204() {
        var headers = authHeaders(rest);

        var req = new CreateCommissionBonusTierRequest(
                BigDecimal.valueOf(200000), BigDecimal.valueOf(300000), BigDecimal.valueOf(2.5));
        var created = rest.exchange("/commission-bonus-tiers", HttpMethod.POST,
                new HttpEntity<>(req, headers), CommissionBonusTierResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long tierId = created.getBody().id();

        var deleteResp = rest.exchange("/commission-bonus-tiers/" + tierId, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify the tier is no longer active (should not appear in active list)
        var listResp = rest.exchange("/commission-bonus-tiers", HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<CommissionBonusTierResponse>>() {});
        assertThat(listResp.getBody()).noneMatch(t -> t.id().equals(tierId) && t.active());
    }

    @Test
    void createOverlappingCommissionBonusTierReturns409Or422() {
        var headers = authHeaders(rest);

        // Create first tier: [300000 + offset, 800000 + offset] to avoid collision with other tests
        long offset = System.nanoTime() % 1_000_000;
        BigDecimal min1 = BigDecimal.valueOf(400000 + offset);
        BigDecimal max1 = BigDecimal.valueOf(900000 + offset);

        var first = rest.exchange("/commission-bonus-tiers", HttpMethod.POST,
                new HttpEntity<>(new CreateCommissionBonusTierRequest(min1, max1, BigDecimal.valueOf(1.0)), headers),
                CommissionBonusTierResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Create overlapping tier: [600000 + offset, 1100000 + offset]
        BigDecimal min2 = BigDecimal.valueOf(600000 + offset);
        BigDecimal max2 = BigDecimal.valueOf(1100000 + offset);

        var second = rest.exchange("/commission-bonus-tiers", HttpMethod.POST,
                new HttpEntity<>(new CreateCommissionBonusTierRequest(min2, max2, BigDecimal.valueOf(1.5)), headers),
                String.class);
        assertThat(second.getStatusCode().value()).isIn(409, 422);
    }
}
