package github.io.ddmfuhrmann.outfit.sales;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockMovementResponse;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.party.application.dto.CreatePartyRequest;
import github.io.ddmfuhrmann.outfit.party.application.dto.PartyCreatedResponse;
import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;
import github.io.ddmfuhrmann.outfit.sales.application.dto.*;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsignmentControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    private record TestSetup(Long skuIdA, Long productIdA, Long skuIdB, Long productIdB,
                             Long customerId, Long salespersonId) {}

    private TestSetup setup(HttpHeaders headers) {
        long ts = System.nanoTime();

        var brandId = createBrand(headers, ts);
        var categoryId = createCategory(headers, ts);
        var sizeId = createSize(headers, ts);

        var productA = createProduct(headers, brandId, categoryId, sizeId, "CSN-A-" + ts, 50);
        var productB = createProduct(headers, brandId, categoryId, sizeId, "CSN-B-" + ts, 30);

        var customerId = createParty(headers, "52998224725", true, false, false);
        var salespersonId = createParty(headers, "11144477735", false, false, true);

        return new TestSetup(
                productA.skus().getFirst().id(), productA.id(),
                productB.skus().getFirst().id(), productB.id(),
                customerId, salespersonId
        );
    }

    private Long createBrand(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-" + ts), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-" + ts, null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers, long ts) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Size-" + ts), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                          Long sizeId, String barcode, int implantationQty) {
        var request = new CreateProductRequest(
                "IT Product " + barcode, BigDecimal.valueOf(200.00), BigDecimal.valueOf(100.00),
                null, null, brandId, categoryId,
                List.of(new CreateSkuRequest(barcode, sizeId, implantationQty))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    private Long createParty(HttpHeaders headers, String cpf, boolean customer,
                              boolean supplier, boolean salesperson) {
        var request = new CreatePartyRequest(
                PersonType.INDIVIDUAL, null, cpf,
                "Party " + cpf, "Party " + cpf,
                customer, supplier, salesperson,
                salesperson ? BigDecimal.valueOf(5.0) : null,
                null, null
        );
        var resp = rest.exchange("/party", HttpMethod.POST,
                new HttpEntity<>(request, headers), PartyCreatedResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private IssueConsignmentRequest buildIssueRequest(TestSetup s) {
        return new IssueConsignmentRequest(
                s.customerId(),
                List.of(s.salespersonId()),
                LocalDate.now(),
                "test consignment",
                List.of(
                        new ConsignmentItemRequest(s.skuIdA(), s.productIdA(), 5, BigDecimal.valueOf(150.00)),
                        new ConsignmentItemRequest(s.skuIdB(), s.productIdB(), 3, BigDecimal.valueOf(200.00))
                )
        );
    }

    private long issueConsignment(HttpHeaders headers, TestSetup s) {
        var resp = rest.exchange("/consignments", HttpMethod.POST,
                new HttpEntity<>(buildIssueRequest(s), headers), ConsignmentResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    @Test
    void issueConsignmentReturns201() {
        var headers = authHeaders(rest);
        var s = setup(headers);

        var resp = rest.exchange("/consignments", HttpMethod.POST,
                new HttpEntity<>(buildIssueRequest(s), headers), ConsignmentResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).isNotNull();

        var body = resp.getBody();
        assertThat(body.customerId()).isEqualTo(s.customerId());
        assertThat(body.status()).isEqualTo("OPEN");

        var itemA = body.items().stream().filter(i -> i.skuId().equals(s.skuIdA())).findFirst().orElseThrow();
        assertThat(itemA.quantityIssued()).isEqualTo(5);
        assertThat(itemA.quantityReturned()).isZero();
        assertThat(itemA.quantitySold()).isEqualTo(5);

        var itemB = body.items().stream().filter(i -> i.skuId().equals(s.skuIdB())).findFirst().orElseThrow();
        assertThat(itemB.quantityIssued()).isEqualTo(3);
    }

    @Test
    void issueConsignmentDecrementsStockPerItem() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        issueConsignment(headers, s);

        var movementsA = getMovements(headers, s.skuIdA());
        var consignmentDecrementA = movementsA.stream()
                .filter(m -> m.source() == StockSource.CONSIGNMENT && m.quantity() < 0)
                .findFirst();
        assertThat(consignmentDecrementA).isPresent();
        assertThat(consignmentDecrementA.get().quantity()).isEqualTo(-5);

        var movementsB = getMovements(headers, s.skuIdB());
        var consignmentDecrementB = movementsB.stream()
                .filter(m -> m.source() == StockSource.CONSIGNMENT && m.quantity() < 0)
                .findFirst();
        assertThat(consignmentDecrementB).isPresent();
        assertThat(consignmentDecrementB.get().quantity()).isEqualTo(-3);
    }

    @Test
    void returnItemsUpdatesQuantityReturned() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long consignmentId = issueConsignment(headers, s);

        var returnReq = new ReturnItemsRequest(List.of(new ReturnItemRequest(s.skuIdA(), 1)));
        var resp = rest.exchange("/consignments/" + consignmentId + "/return-items", HttpMethod.POST,
                new HttpEntity<>(returnReq, headers), ConsignmentResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var itemA = resp.getBody().items().stream().filter(i -> i.skuId().equals(s.skuIdA())).findFirst().orElseThrow();
        assertThat(itemA.quantityReturned()).isEqualTo(1);
        assertThat(itemA.quantitySold()).isEqualTo(4); // 5 issued - 1 returned
    }

    @Test
    void returnItemsIncrementsStock() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long consignmentId = issueConsignment(headers, s);

        var returnReq = new ReturnItemsRequest(List.of(new ReturnItemRequest(s.skuIdA(), 2)));
        rest.exchange("/consignments/" + consignmentId + "/return-items", HttpMethod.POST,
                new HttpEntity<>(returnReq, headers), Void.class);

        var movements = getMovements(headers, s.skuIdA());
        var returnIncrement = movements.stream()
                .filter(m -> m.source() == StockSource.CONSIGNMENT && m.quantity() > 0)
                .findFirst();
        assertThat(returnIncrement).isPresent();
        assertThat(returnIncrement.get().quantity()).isEqualTo(2);
    }

    @Test
    void returnMoreThanIssuedReturns422() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long consignmentId = issueConsignment(headers, s);

        var returnReq = new ReturnItemsRequest(List.of(new ReturnItemRequest(s.skuIdA(), 99)));
        var resp = rest.exchange("/consignments/" + consignmentId + "/return-items", HttpMethod.POST,
                new HttpEntity<>(returnReq, headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void returnItemsOnClosedConsignmentReturns422() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long consignmentId = issueConsignment(headers, s);

        var closeReq = new CloseConsignmentRequest(
                List.of(s.salespersonId()),
                List.of(new CreateSaleInstallmentRequest("CASH", BigDecimal.valueOf(750.00)))
        );
        rest.exchange("/consignments/" + consignmentId + "/close", HttpMethod.POST,
                new HttpEntity<>(closeReq, headers), Void.class);

        var returnReq = new ReturnItemsRequest(List.of(new ReturnItemRequest(s.skuIdA(), 1)));
        var resp = rest.exchange("/consignments/" + consignmentId + "/return-items", HttpMethod.POST,
                new HttpEntity<>(returnReq, headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Disabled("Complete in phase 4b-2 once CreateSaleUseCase is available")
    void closeConsignmentReturns201WithSale() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long consignmentId = issueConsignment(headers, s);

        var closeReq = new CloseConsignmentRequest(
                List.of(s.salespersonId()),
                List.of(new CreateSaleInstallmentRequest("CASH", BigDecimal.valueOf(750.00)))
        );
        var resp = rest.exchange("/consignments/" + consignmentId + "/close", HttpMethod.POST,
                new HttpEntity<>(closeReq, headers), SaleResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().origin()).isEqualTo("CONSIGNMENT");
        assertThat(resp.getBody().consignmentId()).isEqualTo(consignmentId);
    }

    @Test
    void closeAlreadyClosedConsignmentReturns422() {
        var headers = authHeaders(rest);
        var s = setup(headers);
        long consignmentId = issueConsignment(headers, s);

        var closeReq = new CloseConsignmentRequest(
                List.of(s.salespersonId()),
                List.of(new CreateSaleInstallmentRequest("CASH", BigDecimal.valueOf(750.00)))
        );
        var first = rest.exchange("/consignments/" + consignmentId + "/close", HttpMethod.POST,
                new HttpEntity<>(closeReq, headers), Void.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var second = rest.exchange("/consignments/" + consignmentId + "/close", HttpMethod.POST,
                new HttpEntity<>(closeReq, headers), Void.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private List<StockMovementResponse> getMovements(HttpHeaders headers, Long skuId) {
        var resp = rest.exchange(
                "/inventory/movements/" + skuId + "?size=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<StockMovementResponse>>() {}
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().content();
    }
}
