package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.query.application.dto.ConsignmentDocument;
import github.io.ddmfuhrmann.outfit.sales.application.dto.*;
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

class ConsignmentQueryControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private static final AtomicInteger CPF_SEED = new AtomicInteger(200);

    private record TestSetup(Long skuId, Long productId, Long customerId, Long salespersonId,
                             String customerName) {}

    // --- CPF generation with valid check digits ---

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

    // --- setup helpers ---

    private TestSetup setup(HttpHeaders headers) {
        long ts = System.nanoTime();
        var brandId    = createBrand(headers, ts);
        var categoryId = createCategory(headers, ts);
        var sizeId     = createSize(headers, ts);
        var product    = createProduct(headers, brandId, categoryId, sizeId, "QIT-" + ts, 50);

        String name = "Alice " + ts;
        Long customerId    = createParty(headers, generateCpf(), name, true, false, false);
        Long salespersonId = createParty(headers, generateCpf(), "Seller " + ts, false, false, true);

        return new TestSetup(product.skus().getFirst().id(), product.id(), customerId, salespersonId, name);
    }

    private long issueConsignment(HttpHeaders headers, TestSetup s) {
        var req = new IssueConsignmentRequest(
                s.customerId(),
                List.of(s.salespersonId()),
                LocalDate.now(),
                "IT consignment",
                List.of(new ConsignmentItemRequest(s.skuId(), s.productId(), 5, BigDecimal.valueOf(150.00))));
        var resp = rest.exchange("/consignments", HttpMethod.POST,
                new HttpEntity<>(req, headers), ConsignmentResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private void returnItems(HttpHeaders headers, long consignmentId, Long skuId, int qty) {
        var req = new ReturnItemsRequest(List.of(new ReturnItemRequest(skuId, qty)));
        rest.exchange("/consignments/" + consignmentId + "/return-items", HttpMethod.POST,
                new HttpEntity<>(req, headers), Void.class);
    }

    private void closeConsignment(HttpHeaders headers, long consignmentId, Long salespersonId) {
        var req = new CloseConsignmentRequest(
                List.of(salespersonId),
                List.of(new CreateSaleInstallmentRequest("CASH", BigDecimal.valueOf(750.00))));
        rest.exchange("/consignments/" + consignmentId + "/close", HttpMethod.POST,
                new HttpEntity<>(req, headers), Void.class);
    }

    private ConsignmentDocument getConsignment(HttpHeaders headers, long id) {
        var resp = rest.exchange("/consignments/" + id, HttpMethod.GET,
                new HttpEntity<>(headers), ConsignmentDocument.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    private Long createBrand(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-Q-" + ts), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-Q-" + ts, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Sz-Q-" + ts), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                          Long sizeId, String barcode, int qty) {
        var req = new CreateProductRequest(
                "IT Product " + barcode, BigDecimal.valueOf(200.00), BigDecimal.valueOf(100.00),
                null, null, brandId, categoryId,
                List.of(new CreateSkuRequest(barcode, sizeId, qty)));
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(req, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private Long createParty(HttpHeaders headers, String cpf, String name,
                              boolean customer, boolean supplier, boolean salesperson) {
        var req = new CreatePartyRequest(
                PersonType.INDIVIDUAL, null, cpf,
                name, name,
                customer, supplier, salesperson,
                salesperson ? BigDecimal.valueOf(5.0) : null,
                null, null);
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(req, headers), PartyCreatedResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    // --- tests ---

    @Test
    void getConsignmentReturns200WithEnrichedDocument() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long id = issueConsignment(headers, s);

        var doc = getConsignment(headers, id);

        assertThat(doc.customer().name()).isEqualTo(s.customerName());
        assertThat(doc.sellers()).hasSize(1);
        assertThat(doc.sellers().getFirst().name()).isNotBlank();
        assertThat(doc.items()).hasSize(1);
        assertThat(doc.items().getFirst().productDescription()).isNotBlank();
    }

    @Test
    void getConsignmentAfterReturnReflectsUpdatedQuantities() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long id = issueConsignment(headers, s);
        returnItems(headers, id, s.skuId(), 1);

        var doc = getConsignment(headers, id);

        var item = doc.items().getFirst();
        assertThat(item.quantityReturned()).isEqualTo(1);
        assertThat(item.quantitySold()).isEqualTo(4);
    }

    @Test
    void getConsignmentAfterCloseReflectsStatusClosed() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long id = issueConsignment(headers, s);
        closeConsignment(headers, id, s.salespersonId());

        var doc = getConsignment(headers, id);

        assertThat(doc.status()).isEqualTo("CLOSED");
        assertThat(doc.closedAt()).isNotNull();
    }

    @Test
    void searchConsignmentsByCustomerNameReturnsMatch() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        issueConsignment(headers, s);

        String prefix = s.customerName().substring(0, 5);
        var resp = rest.exchange(
                "/consignments?q=" + prefix + "&size=20",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<ConsignmentDocument>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).anyMatch(d -> d.customerName().equals(s.customerName()));
    }

    @Test
    void searchConsignmentsByStatusFiltersCorrectly() {
        var headers = authHeaders(rest);

        var s1 = setup(headers);
        long openId = issueConsignment(headers, s1);

        var s2 = setup(headers);
        long closedId = issueConsignment(headers, s2);
        closeConsignment(headers, closedId, s2.salespersonId());

        var openResp = rest.exchange(
                "/consignments?status=OPEN&size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<ConsignmentDocument>>() {});

        assertThat(openResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var openContent = openResp.getBody().content();
        assertThat(openContent).anyMatch(d -> d.consignmentId().equals(openId));
        assertThat(openContent).noneMatch(d -> d.consignmentId().equals(closedId));
    }

    @Test
    void getConsignmentNotFoundReturns404() {
        var headers = authHeaders(rest);
        var resp = rest.exchange("/consignments/999999999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
