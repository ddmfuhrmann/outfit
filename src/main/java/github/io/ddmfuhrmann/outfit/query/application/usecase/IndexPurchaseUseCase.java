package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseLineSnapshot;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchasePayableSnapshot;
import github.io.ddmfuhrmann.outfit.query.application.dto.PartyDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseLineDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchasePayableDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IndexPurchaseUseCase {

    private final ElasticsearchClient esClient;
    private final Clock clock;

    public IndexPurchaseUseCase(ElasticsearchClient esClient, Clock clock) {
        this.esClient = esClient;
        this.clock = clock;
    }

    public record PurchaseIndexInput(
            Long purchaseId,
            Long brandId,
            Long supplierId,
            LocalDate purchaseDate,
            String observations,
            String status,
            List<PurchaseLineSnapshot> lines,
            List<PurchasePayableSnapshot> payables) {}

    public void execute(PurchaseIndexInput input) {
        String supplierName = input.supplierId() != null ? resolveDisplayName(input.supplierId()) : null;

        var skuIds = input.lines().stream().map(PurchaseLineSnapshot::productSkuId).toList();
        Map<Long, ProductDocument> productsBySkuId = resolveProductsBySkuIds(skuIds);

        var lineDocuments = input.lines().stream()
                .map(line -> buildLineDocument(line, productsBySkuId.getOrDefault(line.productSkuId(), null)))
                .toList();

        var payableDocuments = input.payables().stream()
                .map(p -> new PurchasePayableDocument(p.payableId(), p.dueDate(), p.amount()))
                .toList();

        BigDecimal totalCost = lineDocuments.stream()
                .map(PurchaseLineDocument::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var doc = new PurchaseDocument(
                input.purchaseId(),
                input.brandId(),
                input.supplierId(),
                supplierName,
                input.purchaseDate(),
                input.observations(),
                input.status(),
                totalCost,
                lineDocuments,
                payableDocuments,
                Instant.now(clock));

        try {
            esClient.index(i -> i
                    .index(ElasticsearchIndexInitializer.INDEX_PURCHASES)
                    .id(input.purchaseId().toString())
                    .document(doc)
                    .refresh(Refresh.True));
            log.info("Indexed purchase {}", input.purchaseId());
        } catch (IOException e) {
            throw new IndexingException("Failed to index purchase " + input.purchaseId(), e);
        }
    }

    private String resolveDisplayName(Long partyId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_PARTIES).id(partyId.toString()),
                    PartyDocument.class);
            if (!response.found() || response.source() == null) return null;
            var party = response.source();
            return (party.name() != null && !party.name().isBlank()) ? party.name() : party.legalName();
        } catch (IOException e) {
            throw new IndexingException("Failed to resolve party display name for " + partyId, e);
        }
    }

    private PurchaseLineDocument buildLineDocument(PurchaseLineSnapshot line, ProductDocument product) {
        String productDescription = product != null ? product.description() : null;
        String brandDescription = (product != null && product.brand() != null) ? product.brand().description() : null;
        BigDecimal totalCost = line.unitCost().multiply(BigDecimal.valueOf(line.quantity()));
        return new PurchaseLineDocument(
                line.productSkuId(),
                productDescription,
                brandDescription,
                line.quantity(),
                line.unitCost(),
                totalCost);
    }

    private Map<Long, ProductDocument> resolveProductsBySkuIds(List<Long> skuIds) {
        if (skuIds.isEmpty()) return Map.of();
        try {
            var values = skuIds.stream().map(FieldValue::of).toList();
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.INDEX_PRODUCTS)
                            .size(skuIds.size())
                            .query(q -> q.terms(t -> t
                                    .field("skus.id")
                                    .terms(tv -> tv.value(values)))),
                    ProductDocument.class);

            Map<Long, ProductDocument> result = new HashMap<>();
            for (var hit : response.hits().hits()) {
                var product = hit.source();
                if (product == null || product.skus() == null) continue;
                for (var sku : product.skus()) {
                    if (skuIds.contains(sku.id())) {
                        result.put(sku.id(), product);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new IndexingException("Failed to resolve products for SKUs " + skuIds, e);
        }
    }
}
