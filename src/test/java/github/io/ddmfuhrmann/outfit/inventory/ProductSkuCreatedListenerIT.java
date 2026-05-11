package github.io.ddmfuhrmann.outfit.inventory;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.*;
import github.io.ddmfuhrmann.outfit.inventory.domain.event.StockEntryRecorded;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockBalanceRepository;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockEntryRepository;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Import(ProductSkuCreatedListenerIT.EventCaptor.class)
class ProductSkuCreatedListenerIT extends AbstractIT {

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
    StockBalanceRepository balanceRepository;

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
    void productSkuCreated_seedsStockBalanceAndEntry() {
        var headers = authHeaders(rest);
        var brandId = createBrand(headers);
        var categoryId = createCategory(headers);
        var sizeId = createSize(headers);

        long ts = System.nanoTime();
        var productA = createProduct(headers, brandId, categoryId, sizeId, "BC-A-" + ts, 10);
        var productB = createProduct(headers, brandId, categoryId, sizeId, "BC-B-" + ts, 0);

        var skuIdA = productA.skus().getFirst().id();
        var skuIdB = productB.skus().getFirst().id();

        // StockBalance exists for both SKUs
        var balanceA = balanceRepository.findById(skuIdA);
        assertThat(balanceA).isPresent();
        assertThat(balanceA.get().getCurrentBalance()).isEqualTo(10);

        var balanceB = balanceRepository.findById(skuIdB);
        assertThat(balanceB).isPresent();
        assertThat(balanceB.get().getCurrentBalance()).isZero();

        // StockEntry exists only for SKU with qty > 0
        var entriesA = entryRepository.findByProductSkuIdOrderByOccurredAtDesc(skuIdA, Pageable.unpaged());
        assertThat(entriesA.getContent()).hasSize(1);
        var entry = entriesA.getContent().getFirst();
        assertThat(entry.getSource()).isEqualTo(StockSource.INITIAL_STOCK);
        assertThat(entry.getQuantity()).isEqualTo(10);
        assertThat(entry.getRunningBalance()).isEqualTo(10);

        var entriesB = entryRepository.findByProductSkuIdOrderByOccurredAtDesc(skuIdB, Pageable.unpaged());
        assertThat(entriesB.getContent()).isEmpty();

        // StockEntryRecorded published only for the positive-qty SKU
        var recorded = captor.getEvents();
        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().skuId()).isEqualTo(skuIdA);
        assertThat(recorded.getFirst().quantity()).isEqualTo(10);
        assertThat(recorded.getFirst().runningBalance()).isEqualTo(10);
        assertThat(recorded.getFirst().source()).isEqualTo(StockSource.INITIAL_STOCK);
    }
}
