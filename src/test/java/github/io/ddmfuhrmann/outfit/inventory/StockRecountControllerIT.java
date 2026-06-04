package github.io.ddmfuhrmann.outfit.inventory;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.AddRecountItemRequest;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.OpenStockRecountRequest;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockRecountResponse;
import github.io.ddmfuhrmann.outfit.inventory.domain.event.StockEntryRecorded;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockRecountStatus;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockEntryRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Import(StockRecountControllerIT.EventCaptor.class)
class StockRecountControllerIT extends AbstractIT {

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

    @Autowired
    StockEntryRepository entryRepository;

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
    void fullRecountFlow() {
        var headers = authHeaders(rest);
        long ts = System.nanoTime();
        var brandId = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId = createSize(headers);

        var productA = createProduct(headers, brandId, categoryId, sizeId, "RCT-A-" + ts, 20);
        var skuA = productA.skus().getFirst().id();

        var productB = createProduct(headers, brandId, categoryId, sizeId, "RCT-B-" + ts, 5);
        var skuB = productB.skus().getFirst().id();

        captor.reset();

        // Open recount → 201
        var openResp = rest.exchange("/inventory/recount", HttpMethod.POST,
                new HttpEntity<>(new OpenStockRecountRequest(LocalDate.of(2025, Month.JUNE, 4), "cycle count"), headers),
                Map.class);
        assertThat(openResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long recountId = Long.parseLong(openResp.getBody().get("id").toString());

        // GET while open → status OPEN
        var getOpen = rest.exchange("/inventory/recount/" + recountId, HttpMethod.GET,
                new HttpEntity<>(headers), StockRecountResponse.class);
        assertThat(getOpen.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getOpen.getBody().status()).isEqualTo(StockRecountStatus.OPEN);

        // Add SKU A (counted 18) → 204
        var addA = rest.exchange("/inventory/recount/" + recountId + "/items", HttpMethod.POST,
                new HttpEntity<>(new AddRecountItemRequest(skuA, 18), headers), Void.class);
        assertThat(addA.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Add SKU B (counted 5) → 204
        var addB = rest.exchange("/inventory/recount/" + recountId + "/items", HttpMethod.POST,
                new HttpEntity<>(new AddRecountItemRequest(skuB, 5), headers), Void.class);
        assertThat(addB.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Add same SKU twice → 400
        var dupResp = rest.exchange("/inventory/recount/" + recountId + "/items", HttpMethod.POST,
                new HttpEntity<>(new AddRecountItemRequest(skuA, 10), headers), Void.class);
        assertThat(dupResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // GET while open → items have systemBalance=null, delta=null
        var getBeforeClose = rest.exchange("/inventory/recount/" + recountId, HttpMethod.GET,
                new HttpEntity<>(headers), StockRecountResponse.class);
        assertThat(getBeforeClose.getStatusCode()).isEqualTo(HttpStatus.OK);
        var itemsBefore = getBeforeClose.getBody().items();
        assertThat(itemsBefore).hasSize(2);
        assertThat(itemsBefore).allMatch(i -> i.systemBalance() == null && i.delta() == null);

        // Close recount → 204
        var closeResp = rest.exchange("/inventory/recount/" + recountId + "/close", HttpMethod.POST,
                new HttpEntity<>(headers), Void.class);
        assertThat(closeResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify StockEntry for SKU A: quantity=-2, source=RECOUNT_ADJUSTMENT, runningBalance=18
        var entriesA = entryRepository.findByProductSkuIdOrderByOccurredAtDesc(skuA,
                org.springframework.data.domain.Pageable.unpaged());
        var adjustmentA = entriesA.getContent().stream()
                .filter(e -> e.getSource() == StockSource.RECOUNT_ADJUSTMENT)
                .findFirst();
        assertThat(adjustmentA).isPresent();
        assertThat(adjustmentA.get().getQuantity()).isEqualTo(-2);
        assertThat(adjustmentA.get().getRunningBalance()).isEqualTo(18);

        // No StockEntry for SKU B (delta=0)
        var entriesB = entryRepository.findByProductSkuIdOrderByOccurredAtDesc(skuB,
                org.springframework.data.domain.Pageable.unpaged());
        var adjustmentB = entriesB.getContent().stream()
                .filter(e -> e.getSource() == StockSource.RECOUNT_ADJUSTMENT)
                .findFirst();
        assertThat(adjustmentB).isEmpty();

        // GET after close → correct systemBalance and delta
        var getAfterClose = rest.exchange("/inventory/recount/" + recountId, HttpMethod.GET,
                new HttpEntity<>(headers), StockRecountResponse.class);
        assertThat(getAfterClose.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = getAfterClose.getBody();
        assertThat(body.status()).isEqualTo(StockRecountStatus.CLOSED);
        assertThat(body.closedAt()).isNotNull();

        var itemA = body.items().stream().filter(i -> i.skuId().equals(skuA)).findFirst().orElseThrow();
        assertThat(itemA.systemBalance()).isEqualTo(20);
        assertThat(itemA.delta()).isEqualTo(-2);

        var itemB = body.items().stream().filter(i -> i.skuId().equals(skuB)).findFirst().orElseThrow();
        assertThat(itemB.systemBalance()).isEqualTo(5);
        assertThat(itemB.delta()).isZero();

        // Close already-closed recount → 422
        var closeAgain = rest.exchange("/inventory/recount/" + recountId + "/close", HttpMethod.POST,
                new HttpEntity<>(headers), Void.class);
        assertThat(closeAgain.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        // Add item to closed recount → 422
        var addToClosed = rest.exchange("/inventory/recount/" + recountId + "/items", HttpMethod.POST,
                new HttpEntity<>(new AddRecountItemRequest(skuA, 5), headers), Void.class);
        assertThat(addToClosed.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        // StockEntryRecorded published for SKU A only
        var events = captor.getEvents();
        var recountEvents = events.stream().filter(e -> e.source() == StockSource.RECOUNT_ADJUSTMENT).toList();
        assertThat(recountEvents).hasSize(1);
        assertThat(recountEvents.getFirst().skuId()).isEqualTo(skuA);
    }
}
