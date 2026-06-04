package github.io.ddmfuhrmann.outfit.inventory;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.ManualAdjustmentRequest;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockMovementResponse;
import github.io.ddmfuhrmann.outfit.inventory.domain.event.StockEntryRecorded;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockBalanceRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Import(InventoryControllerIT.EventCaptor.class)
class InventoryControllerIT extends AbstractIT {

    @TestConfiguration
    static class EventCaptor {
        private final CopyOnWriteArrayList<StockEntryRecorded> captured = new CopyOnWriteArrayList<>();

        @EventListener
        public void on(StockEntryRecorded event) {
            captured.add(event);
        }

        public List<StockEntryRecorded> getEvents() {
            return new ArrayList<>(captured);
        }

        public void reset() {
            captured.clear();
        }
    }

    @Autowired
    TestRestTemplate rest;

    private static final Instant ADJUSTMENT_TIME = Instant.parse("2025-06-04T01:00:00Z");

    @Autowired
    StockBalanceRepository balanceRepository;

    @Autowired
    EventCaptor captor;

    @BeforeEach
    void resetCaptor() {
        captor.reset();
    }

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

    private ProductResponse createProduct(HttpHeaders headers, Long brandId, Long categoryId,
                                          Long sizeId, String barcode, int implantationQty) {
        var request = new CreateProductRequest(
                "IT Shirt " + barcode, BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00),
                null, null, brandId, categoryId,
                List.of(new CreateSkuRequest(barcode, sizeId, implantationQty))
        );
        var resp = rest.exchange("/catalog/products", HttpMethod.POST,
                new HttpEntity<>(request, headers), ProductResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    @Test
    void manualAdjustment_increasesBalance_andRecordsEntry() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId = createSize(headers);
        var product = createProduct(headers, brandId, categoryId, sizeId, "ADJ-" + ts, 20);
        var skuId = product.skus().getFirst().id();

        captor.reset();

        var adjustRequest = new ManualAdjustmentRequest(skuId, 25, ADJUSTMENT_TIME);
        var resp = rest.exchange("/inventory/adjustment", HttpMethod.POST,
                new HttpEntity<>(adjustRequest, headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var balance = balanceRepository.findById(skuId);
        assertThat(balance).isPresent();
        assertThat(balance.get().getCurrentBalance()).isEqualTo(25);

        var events = captor.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().source()).isEqualTo(StockSource.MANUAL_ADJUSTMENT);
        assertThat(events.getFirst().quantity()).isEqualTo(5);
        assertThat(events.getFirst().runningBalance()).isEqualTo(25);
    }

    @Test
    void manualAdjustment_sameBalance_returns422() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId = createSize(headers);
        var product = createProduct(headers, brandId, categoryId, sizeId, "SAME-" + ts, 20);
        var skuId = product.skus().getFirst().id();

        var adjustRequest = new ManualAdjustmentRequest(skuId, 20, ADJUSTMENT_TIME);
        var resp = rest.exchange("/inventory/adjustment", HttpMethod.POST,
                new HttpEntity<>(adjustRequest, headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void manualAdjustment_unknownSku_returns404() {
        var headers = authHeaders(rest);
        var adjustRequest = new ManualAdjustmentRequest(999_999_999L, 10, ADJUSTMENT_TIME);
        var resp = rest.exchange("/inventory/adjustment", HttpMethod.POST,
                new HttpEntity<>(adjustRequest, headers), Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getMovements_returnsEntriesOrderedByOccurredAtDesc() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId = createSize(headers);
        var product = createProduct(headers, brandId, categoryId, sizeId, "MOV-" + ts, 20);
        var skuId = product.skus().getFirst().id();

        var adjustRequest = new ManualAdjustmentRequest(skuId, 25, ADJUSTMENT_TIME);
        rest.exchange("/inventory/adjustment", HttpMethod.POST,
                new HttpEntity<>(adjustRequest, headers), Void.class);

        var resp = rest.exchange("/inventory/movements/" + skuId + "?page=0&size=10",
                HttpMethod.GET, new HttpEntity<>(headers),
                new ParameterizedTypeReference<PageResponse<StockMovementResponse>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalElements()).isEqualTo(2);

        var entries = body.content();
        assertThat(entries.getFirst().source()).isEqualTo(StockSource.MANUAL_ADJUSTMENT);
        assertThat(entries.get(1).source()).isEqualTo(StockSource.INITIAL_STOCK);
    }
}
