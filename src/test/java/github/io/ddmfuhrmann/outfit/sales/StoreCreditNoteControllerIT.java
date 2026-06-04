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

class StoreCreditNoteControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private static final AtomicInteger CPF_SEED = new AtomicInteger(800);

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

    private TestSetup setup(HttpHeaders headers, int implantationQty) {
        long ts = System.nanoTime();
        var brandId    = createBrand(headers, ts);
        var categoryId = createCategory(headers, ts);
        var sizeId     = createSize(headers, ts);
        var product    = createProduct(headers, brandId, categoryId, sizeId, "SCR-" + ts, implantationQty);

        Long customerId    = createParty(headers, generateCpf(), "Customer " + ts, true, false, false, null);
        Long salespersonId = createParty(headers, generateCpf(), "Seller " + ts, false, false, true, BigDecimal.valueOf(5.0));

        return new TestSetup(product.skus().getFirst().id(), product.id(), customerId, salespersonId);
    }

    private Long createBrand(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-SCR-" + ts), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-SCR-" + ts, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-SCR-" + ts), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                          Long sizeId, String barcode, int qty) {
        var req = new CreateProductRequest(
                "Product " + barcode, BigDecimal.valueOf(120.00), BigDecimal.valueOf(60.00),
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

    private StoreCreditNoteResponse createNote(HttpHeaders headers, TestSetup s, int quantity, BigDecimal unitPrice) {
        var req = new CreateStoreCreditNoteRequest(
                s.customerId(),
                "return note",
                List.of(new StoreCreditItemRequest(s.skuId(), s.productId(), quantity, unitPrice)));
        var resp = rest.exchange("/store-credit-notes", HttpMethod.POST,
                new HttpEntity<>(req, headers), StoreCreditNoteResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private SaleResponse createSaleWithCreditNote(HttpHeaders headers, TestSetup s, Long storeCreditNoteId) {
        // Sale: 3 × 100 = 300 gross; credit note discount applied
        var req = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                LocalDate.now(),
                storeCreditNoteId,
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 3, BigDecimal.valueOf(100.00))),
                // netAmount will be 300 - discount; installment must match netAmount
                // We supply full gross here and let the use case cap the discount.
                // For a note of 50 (totalAmount), discount = 50, netAmount = 250
                List.of(new CreateSaleInstallmentRequest(PaymentModality.PIX, LocalDate.now(), BigDecimal.valueOf(250.00))),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));
        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(req, headers), SaleResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
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
    void createStoreCreditNoteReturns201() {
        var headers = authHeaders(rest);
        var s = setup(headers, 20);

        var req = new CreateStoreCreditNoteRequest(
                s.customerId(),
                "test return",
                List.of(new StoreCreditItemRequest(s.skuId(), s.productId(), 2, BigDecimal.valueOf(100.00))));
        var resp = rest.exchange("/store-credit-notes", HttpMethod.POST,
                new HttpEntity<>(req, headers), StoreCreditNoteResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).isNotNull();

        var body = resp.getBody();
        assertThat(body.id()).isNotNull();
        assertThat(body.customerId()).isEqualTo(s.customerId());
        assertThat(body.status()).isEqualTo("OPEN");
        assertThat(body.totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(body.items()).hasSize(1);
    }

    @Test
    void getStoreCreditNoteReturns200() {
        var headers = authHeaders(rest);
        var s = setup(headers, 20);
        var created = createNote(headers, s, 1, BigDecimal.valueOf(120.00));

        var resp = rest.exchange("/store-credit-notes/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(headers), StoreCreditNoteResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().id()).isEqualTo(created.id());
        assertThat(resp.getBody().status()).isEqualTo("OPEN");
    }

    @Test
    void listStoreCreditNotesByCustomerIdFilters() {
        var headers = authHeaders(rest);
        var s1 = setup(headers, 30);
        var s2 = setup(headers, 30);
        createNote(headers, s1, 1, BigDecimal.valueOf(100.00));
        createNote(headers, s2, 1, BigDecimal.valueOf(100.00));

        var resp = rest.exchange(
                "/store-credit-notes?customerId=" + s1.customerId() + "&size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<StoreCreditNoteResponse>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = resp.getBody().content();
        assertThat(content).allMatch(n -> n.customerId().equals(s1.customerId()));
        assertThat(content).noneMatch(n -> n.customerId().equals(s2.customerId()));
    }

    @Test
    void applyStoreCreditNoteDiscountOnSale() {
        var headers = authHeaders(rest);
        // 20 units in stock so both note and sale can proceed
        var s = setup(headers, 20);

        // Note for 50 → discount will be min(50, 300) = 50
        var note = createNote(headers, s, 1, BigDecimal.valueOf(50.00));
        assertThat(note.totalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

        var sale = createSaleWithCreditNote(headers, s, note.id());

        assertThat(sale.storeCreditDiscount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(sale.grossAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(sale.netAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(sale.storeCreditNoteId()).isEqualTo(note.id());
    }

    @Test
    void storeCreditNoteStockIncremented() {
        var headers = authHeaders(rest);
        var s = setup(headers, 10);

        // Create note returning 2 units
        createNote(headers, s, 2, BigDecimal.valueOf(100.00));

        var movements = getMovements(headers, s.skuId());
        var returnIncrement = movements.stream()
                .filter(m -> m.source() == StockSource.RETURN && m.quantity() > 0)
                .findFirst();
        assertThat(returnIncrement).isPresent();
        assertThat(returnIncrement.get().quantity()).isEqualTo(2);
    }

    @Test
    void consumedStoreCreditNoteCannotBeReusedReturns422() {
        var headers = authHeaders(rest);
        var s = setup(headers, 20);

        // note = 50 → used in a sale (netAmount = 250, installment = 250)
        var note = createNote(headers, s, 1, BigDecimal.valueOf(50.00));
        createSaleWithCreditNote(headers, s, note.id());

        // Try to use same note again in another sale
        var req = new CreateSaleRequest(
                s.customerId(),
                SaleOrigin.DIRECT,
                null,
                LocalDate.now(),
                note.id(),
                null,
                null,
                List.of(new CreateSaleItemRequest(s.skuId(), s.productId(), 1, BigDecimal.valueOf(100.00))),
                List.of(new CreateSaleInstallmentRequest(PaymentModality.PIX, LocalDate.now(), BigDecimal.valueOf(100.00))),
                List.of(new CreateSaleSellerRequest(s.salespersonId(), new BigDecimal("100"))));
        var resp = rest.exchange("/sales", HttpMethod.POST,
                new HttpEntity<>(req, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
