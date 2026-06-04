package github.io.ddmfuhrmann.outfit.sales;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockMovementResponse;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
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

class SaleControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private static final AtomicInteger CPF_SEED = new AtomicInteger(400);

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
        var product    = createProduct(headers, brandId, categoryId, sizeId, "SALE-" + ts, 50);

        Long customerId     = createParty(headers, generateCpf(), "Customer " + ts, true, false, false, null);
        Long salespersonId  = createParty(headers, generateCpf(), "Seller1 " + ts, false, false, true, BigDecimal.valueOf(5.0));
        Long salespersonId2 = createParty(headers, generateCpf(), "Seller2 " + ts, false, false, true, BigDecimal.valueOf(5.0));

        return new TestSetup(product.skus().getFirst().id(), product.id(), customerId, salespersonId, salespersonId2);
    }

    private Long createBrand(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-S-" + ts), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-S-" + ts, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-S-" + ts), headers), SizeResponse.class);
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

    private List<StockMovementResponse> getMovements(HttpHeaders headers, Long skuId) {
        var resp = rest.exchange(
                "/inventory/movements/" + skuId + "?size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<StockMovementResponse>>() {});
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().content();
    }

    @Test
    void createSaleReturns201() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        var request = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                LocalDate.now(),
                null,
                null,
                "IT direct sale",
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 3, BigDecimal.valueOf(100.00))),
                List.of(new CreateSaleInstallmentRequest(PaymentModality.PIX, LocalDate.now(), BigDecimal.valueOf(300.00))),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));

        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(request, headers), SaleResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).isNotNull();

        var body = resp.getBody();
        assertThat(body.id()).isNotNull();
        assertThat(body.customerId()).isEqualTo(s.customerId());
        assertThat(body.origin()).isEqualTo("DIRECT");
        assertThat(body.grossAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(body.netAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(body.items()).hasSize(1);
        assertThat(body.installments()).hasSize(1);
        assertThat(body.sellers()).hasSize(1);
    }

    @Test
    void createDirectSaleDecrementsStock() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        var request = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 5, BigDecimal.valueOf(80.00))),
                List.of(new CreateSaleInstallmentRequest(PaymentModality.CASH, LocalDate.now(), BigDecimal.valueOf(400.00))),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));

        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(request, headers), SaleResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var movements = getMovements(headers, s.skuId());
        var saleDecrement = movements.stream()
                .filter(m -> m.source() == StockSource.SALE && m.quantity() < 0)
                .findFirst();
        assertThat(saleDecrement).isPresent();
        assertThat(saleDecrement.get().quantity()).isEqualTo(-5);
    }

    @Test
    void createSaleWithTwoSellersValidates() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        var request = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 2, BigDecimal.valueOf(150.00))),
                List.of(new CreateSaleInstallmentRequest(PaymentModality.DEBIT_CARD, LocalDate.now(), BigDecimal.valueOf(300.00))),
                List.of(
                        new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("60")),
                        new CreateSaleSellerRequest(s.salespersonId2(), new BigDecimal("40"))));

        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(request, headers), SaleResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().sellers()).hasSize(2);
    }

    @Test
    void createSaleInstallmentMismatchReturns422() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        // grossAmount = 3 × 100 = 300, but installment sums to 200 — mismatch
        var request = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 3, BigDecimal.valueOf(100.00))),
                List.of(new CreateSaleInstallmentRequest(PaymentModality.PIX, LocalDate.now(), BigDecimal.valueOf(200.00))),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));

        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(request, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
