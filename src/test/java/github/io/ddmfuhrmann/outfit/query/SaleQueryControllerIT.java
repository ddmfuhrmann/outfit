package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.query.application.dto.SaleDocument;
import github.io.ddmfuhrmann.outfit.sales.application.dto.*;
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

class SaleQueryControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private static final AtomicInteger CPF_SEED = new AtomicInteger(700);

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
                             String customerName, String salespersonName) {}

    private TestSetup setup(HttpHeaders headers) {
        long ts = System.nanoTime();
        var brandId    = createBrand(headers, ts);
        var categoryId = createCategory(headers, ts);
        var sizeId     = createSize(headers, ts);
        var product    = createProduct(headers, brandId, categoryId, sizeId, "SQ-" + ts, 50);

        String customerName   = "Buyer " + ts;
        String salespersonName = "Agent " + ts;
        Long customerId    = createParty(headers, generateCpf(), customerName, true, false, false, null);
        Long salespersonId = createParty(headers, generateCpf(), salespersonName, false, false, true, BigDecimal.valueOf(5.0));

        return new TestSetup(product.skus().getFirst().id(), product.id(), customerId, salespersonId,
                customerName, salespersonName);
    }

    private Long createBrand(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-SQ-" + ts), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-SQ-" + ts, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-SQ-" + ts), headers), SizeResponse.class);
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

    private SaleResponse createSale(HttpHeaders headers, TestSetup s) {
        return createSale(headers, s, LocalDate.now());
    }

    private SaleResponse createSale(HttpHeaders headers, TestSetup s, LocalDate issueDate) {
        var request = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                issueDate,
                null,
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 2, BigDecimal.valueOf(150.00))),
                List.of(new CreateSaleInstallmentRequest(PaymentModality.PIX, issueDate, BigDecimal.valueOf(300.00))),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));

        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(request, headers), SaleResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private SaleDocument getSale(HttpHeaders headers, Long id) {
        var resp = rest.exchange("/sales/" + id, HttpMethod.GET,
                new HttpEntity<>(headers), SaleDocument.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @Test
    void getSaleReturns200WithEnrichedDocument() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        var sale = createSale(headers, s);

        var doc = getSale(headers, sale.id());

        assertThat(doc.saleId()).isEqualTo(sale.id());
        assertThat(doc.customer()).isNotNull();
        assertThat(doc.customer().name()).isEqualTo(s.customerName());
        assertThat(doc.sellers()).hasSize(1);
        assertThat(doc.sellers().getFirst().name()).isNotBlank();
        assertThat(doc.items()).hasSize(1);
        assertThat(doc.items().getFirst().productDescription()).isNotBlank();
    }

    @Test
    void getSaleNotFoundReturns404() {
        var headers = authHeaders(rest);
        var resp = rest.exchange("/sales/999999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchSalesByCustomerNameReturnsMatch() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        createSale(headers, s);

        String prefix = s.customerName().substring(0, 5);
        var resp = rest.exchange(
                "/sales?q=" + prefix + "&size=20",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<SaleDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).anyMatch(d -> d.customerName().equals(s.customerName()));
    }

    @Test
    void searchSalesByCustomerIdFiltersCorrectly() {
        var headers = authHeaders(rest);
        var s1 = setup(headers);
        var s2 = setup(headers);
        createSale(headers, s1);
        createSale(headers, s2);

        var resp = rest.exchange(
                "/sales?customerId=" + s1.customerId() + "&size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<SaleDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = resp.getBody().content();
        assertThat(content).allMatch(d -> d.customerId().equals(s1.customerId()));
        assertThat(content).noneMatch(d -> d.customerId().equals(s2.customerId()));
    }

    @Test
    void searchSalesBySellerIdFiltersCorrectly() {
        var headers = authHeaders(rest);
        var s1 = setup(headers);
        var s2 = setup(headers);
        createSale(headers, s1);
        createSale(headers, s2);

        var resp = rest.exchange(
                "/sales?salespersonId=" + s1.salespersonId() + "&size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<SaleDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = resp.getBody().content();
        assertThat(content).anyMatch(d ->
                d.sellers().stream().anyMatch(sel -> sel.id().equals(s1.salespersonId())));
        assertThat(content).noneMatch(d ->
                d.sellers().stream().anyMatch(sel -> sel.id().equals(s2.salespersonId())));
    }

    @Test
    void searchSalesByDateRangeFiltersCorrectly() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        LocalDate targetDate = LocalDate.of(2026, 1, 15);
        var sale = createSale(headers, s, targetDate);

        var resp = rest.exchange(
                "/sales?from=2026-01-15&to=2026-01-15&size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<SaleDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).anyMatch(d -> d.saleId().equals(sale.id()));
    }

    @Test
    void getSaleReflectsCorrectAmounts() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        // 2 items × 150 = 300, no discount
        var sale = createSale(headers, s);

        var doc = getSale(headers, sale.id());

        assertThat(doc.grossAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(doc.netAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(doc.storeCreditDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
