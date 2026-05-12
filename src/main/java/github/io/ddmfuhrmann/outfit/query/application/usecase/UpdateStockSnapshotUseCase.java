package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.inventory.domain.event.StockEntryRecorded;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductRefDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductSkuDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockSnapshotDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class UpdateStockSnapshotUseCase {

    private final ElasticsearchClient esClient;

    public UpdateStockSnapshotUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(StockEntryRecorded event) {
        try {
            String docId = event.skuId().toString();
            var existing = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.STOCK_SNAPSHOT_INDEX).id(docId),
                    StockSnapshotDocument.class);

            if (existing.found()) {
                partialUpdate(docId, event);
            } else {
                fullIndex(docId, event);
            }
            log.info("Updated stock snapshot for SKU {}", event.skuId());
        } catch (IOException e) {
            throw new IndexingException("Failed to update stock snapshot for SKU " + event.skuId(), e);
        }
    }

    private void partialUpdate(String docId, StockEntryRecorded event) throws IOException {
        esClient.update(u -> u
                .index(ElasticsearchIndexInitializer.STOCK_SNAPSHOT_INDEX)
                .id(docId)
                .doc(Map.of(
                        ElasticsearchIndexInitializer.FIELD_CURRENT_BALANCE, event.runningBalance(),
                        ElasticsearchIndexInitializer.FIELD_UPDATED_AT, event.occurredAt().toString()))
                .refresh(Refresh.True),
                StockSnapshotDocument.class);
    }

    private void fullIndex(String docId, StockEntryRecorded event) throws IOException {
        var product = fetchProductWithSku(event.productId(), event.skuId());
        var skuDoc = product.skus().stream().filter(s -> s.id().equals(event.skuId())).findFirst().orElse(null);
        var doc = buildDocument(event, product, skuDoc);

        esClient.index(i -> i
                .index(ElasticsearchIndexInitializer.STOCK_SNAPSHOT_INDEX)
                .id(docId)
                .document(doc)
                .refresh(Refresh.True));
    }

    private StockSnapshotDocument buildDocument(StockEntryRecorded event, ProductDocument product,
                                                 ProductSkuDocument sku) {
        return new StockSnapshotDocument(
                event.skuId(),
                sku != null ? sku.barcode() : null,
                sku != null ? sku.sizeId() : null,
                sku != null ? sku.sizeDescription() : null,
                event.productId(),
                product.description(),
                product.active(),
                refId(product.brand()),   refDesc(product.brand()),
                refId(product.category()), refDesc(product.category()),
                refId(product.color()),    refDesc(product.color()),
                event.runningBalance(),
                event.occurredAt());
    }

    private ProductDocument fetchProductWithSku(Long productId, Long skuId) throws IOException {
        Retry retry = Retry.of("fetchProductWithSku",
                RetryConfig.<ProductDocument>custom()
                        .maxAttempts(6)
                        .waitDuration(Duration.ofMillis(200))
                        .retryOnResult(p -> p == null
                                || p.skus().stream().noneMatch(s -> s.id().equals(skuId)))
                        .build());
        try {
            return retry.executeCallable(() -> {
                var response = esClient.get(
                        g -> g.index(ElasticsearchIndexInitializer.INDEX_PRODUCTS).id(productId.toString()),
                        ProductDocument.class);
                return response.found() ? response.source() : null;
            });
        } catch (Exception e) {
            if (e instanceof IOException ioe) throw ioe;
            throw new IndexingException("Failed to fetch product " + productId + " from index", e);
        }
    }

    private static Long   refId(ProductRefDocument ref)   { return ref != null ? ref.id()          : null; }
    private static String refDesc(ProductRefDocument ref) { return ref != null ? ref.description() : null; }
}
