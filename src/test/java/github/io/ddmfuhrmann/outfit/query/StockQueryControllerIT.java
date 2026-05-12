package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.ManualAdjustmentRequest;
import github.io.ddmfuhrmann.outfit.query.application.dto.BulkBalanceRequest;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockMonthlyDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockSnapshotDocument;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

class StockQueryControllerIT extends AbstractIT {

    private static final ParameterizedTypeReference<PageResponse<StockSnapshotDocument>> SNAPSHOT_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<StockMonthlyDocument>> MONTHLY_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<StockSnapshotDocument>> SNAPSHOT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    TestRestTemplate rest;

    // --- setup helpers ---

    private Long createBrand(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Brand-" + System.nanoTime()), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createCategory(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/categories", HttpMethod.POST,
                new HttpEntity<>(new CategoryRequest("Cat-" + System.nanoTime(), null), headers), CategoryResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createSize(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/sizes", HttpMethod.POST,
                new HttpEntity<>(new SizeRequest("Size-" + System.nanoTime()), headers), SizeResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private Long createColor(HttpHeaders headers) {
        var resp = rest.exchange("/catalog/colors", HttpMethod.POST,
                new HttpEntity<>(new ColorRequest("Color-" + System.nanoTime()), headers), ColorResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                           Long colorId, Long sizeId, int implantationQty) {
        String barcode = "BAR-" + System.nanoTime();
        var request = new CreateProductRequest(
                "Product " + barcode, BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                null, colorId, brandId, categoryId,
                List.of(new CreateSkuRequest(barcode, sizeId, implantationQty)));
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    // --- fetch helpers (throw AssertionError when not yet indexed — composable with untilAsserted) ---

    private StockSnapshotDocument fetchSnapshot(Long skuId) {
        var resp = rest.exchange("/inventory/balance/" + skuId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), StockSnapshotDocument.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    private StockMonthlyDocument fetchMonthlyFirst(Long skuId, String yearMonth) {
        var resp = rest.exchange(
                "/inventory/stock/monthly?skuId=" + skuId + "&yearMonth=" + yearMonth + "&page=0&size=10",
                HttpMethod.GET, new HttpEntity<>(authHeaders(rest)), MONTHLY_PAGE);
        assertThat(resp.getBody().content()).isNotEmpty();
        return resp.getBody().content().getFirst();
    }

    // --- await helpers ---

    private StockSnapshotDocument awaitSnapshot(Long skuId) {
        var ref = new AtomicReference<StockSnapshotDocument>();
        await().atMost(3, SECONDS).untilAsserted(() -> ref.set(fetchSnapshot(skuId)));
        return ref.get();
    }

    private static String currentMonth() {
        return YearMonth.now(ZoneOffset.UTC).format(YEAR_MONTH_FORMAT);
    }

    // --- tests ---

    @Test
    void afterSkuCreated_snapshotShowsImplantationBalance_withAllReferenceFields() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var colorId    = createColor(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, colorId, sizeId, 10);
        var skuId      = product.skus().getFirst().id();

        await().atMost(3, SECONDS).untilAsserted(() -> {
            var doc = fetchSnapshot(skuId);
            assertThat(doc.currentBalance()).isEqualTo(10);
            assertThat(doc.skuId()).isEqualTo(skuId);
            assertThat(doc.productId()).isEqualTo(product.id());
            assertThat(doc.productDescription()).isNotBlank();
            assertThat(doc.brandId()).isEqualTo(brandId);
            assertThat(doc.categoryId()).isEqualTo(categoryId);
            assertThat(doc.colorId()).isEqualTo(colorId);
            assertThat(doc.barcode()).isNotBlank();
            assertThat(doc.sizeId()).isEqualTo(sizeId);
        });
    }

    @Test
    void unknownSkuReturns404() {
        var resp = rest.exchange("/inventory/balance/999999999", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void afterManualAdjustment_snapshotUpdatesBalance() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 10);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        rest.exchange("/inventory/adjustment", HttpMethod.POST,
                new HttpEntity<>(new ManualAdjustmentRequest(skuId, 25, Instant.now()), headers), Void.class);

        await().atMost(3, SECONDS).untilAsserted(() ->
                assertThat(fetchSnapshot(skuId).currentBalance()).isEqualTo(25));
    }

    @Test
    void searchByProductId_returnsSnapshotsForThatProduct() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 5);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        var resp = rest.exchange("/inventory/balance?productId=" + product.id() + "&page=0&size=20",
                HttpMethod.GET, new HttpEntity<>(authHeaders(rest)), SNAPSHOT_PAGE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).extracting(StockSnapshotDocument::skuId).contains(skuId);
    }

    @Test
    void searchByBrandId_returnsSnapshotsForThatBrand() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 5);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        var resp = rest.exchange("/inventory/balance?brandId=" + brandId + "&page=0&size=20",
                HttpMethod.GET, new HttpEntity<>(authHeaders(rest)), SNAPSHOT_PAGE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).extracting(StockSnapshotDocument::skuId).contains(skuId);
    }

    @Test
    void searchByCategoryId_returnsSnapshotsForThatCategory() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 5);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        var resp = rest.exchange("/inventory/balance?categoryId=" + categoryId + "&page=0&size=20",
                HttpMethod.GET, new HttpEntity<>(authHeaders(rest)), SNAPSHOT_PAGE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).extracting(StockSnapshotDocument::skuId).contains(skuId);
    }

    @Test
    void searchWithNoParams_returnsAllSnapshotsPaginated() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 3);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        var resp = rest.exchange("/inventory/balance?page=0&size=100",
                HttpMethod.GET, new HttpEntity<>(authHeaders(rest)), SNAPSHOT_PAGE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().totalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void bulkFetch_returnsBothDocuments_unknownSkuOmitted() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 7);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        long unknownSkuId = 999_000_000_999L;
        var resp = rest.exchange("/inventory/balance/bulk", HttpMethod.POST,
                new HttpEntity<>(new BulkBalanceRequest(List.of(skuId, unknownSkuId)), authHeaders(rest)),
                SNAPSHOT_LIST);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().getFirst().skuId()).isEqualTo(skuId);
    }

    @Test
    void monthlyDocument_createdWithCorrectBalances() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 10);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        await().atMost(3, SECONDS).untilAsserted(() -> {
            var monthly = fetchMonthlyFirst(skuId, currentMonth());
            assertThat(monthly.openingBalance()).isZero();
            assertThat(monthly.totalInbound()).isEqualTo(10);
            assertThat(monthly.closingBalance()).isEqualTo(10);
        });
    }

    @Test
    void twoMovementsInSameMonth_monthlyAccumulatesBoth() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 10);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);

        rest.exchange("/inventory/adjustment", HttpMethod.POST,
                new HttpEntity<>(new ManualAdjustmentRequest(skuId, 25, Instant.now()), headers), Void.class);

        await().atMost(3, SECONDS).untilAsserted(() ->
                assertThat(fetchSnapshot(skuId).currentBalance()).isEqualTo(25));

        await().atMost(3, SECONDS).untilAsserted(() -> {
            var monthly = fetchMonthlyFirst(skuId, currentMonth());
            assertThat(monthly.closingBalance()).isEqualTo(25);
            assertThat(monthly.totalInbound()).isEqualTo(25);
        });
    }

    @Test
    void searchMonthlyByBrandId_returnsOnlyThatBrand() {
        var headers    = authHeaders(rest);
        var brandId    = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId     = createSize(headers);
        var product    = createProduct(headers, brandId, categoryId, null, sizeId, 8);
        var skuId      = product.skus().getFirst().id();

        awaitSnapshot(skuId);
        await().atMost(3, SECONDS).untilAsserted(() -> fetchMonthlyFirst(skuId, currentMonth()));

        var resp = rest.exchange("/inventory/stock/monthly?brandId=" + brandId + "&page=0&size=20",
                HttpMethod.GET, new HttpEntity<>(authHeaders(rest)), MONTHLY_PAGE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().content()).extracting(StockMonthlyDocument::skuId).contains(skuId);
    }
}
