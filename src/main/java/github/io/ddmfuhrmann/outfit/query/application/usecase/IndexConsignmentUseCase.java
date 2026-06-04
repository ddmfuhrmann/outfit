package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.query.application.dto.*;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentIssued;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
public class IndexConsignmentUseCase {

    private final ElasticsearchClient esClient;
    private final Clock clock;

    public IndexConsignmentUseCase(ElasticsearchClient esClient, Clock clock) {
        this.esClient = esClient;
        this.clock = clock;
    }

    public void execute(ConsignmentIssued event) {
        String customerName = resolveDisplayName(event.customerId());
        var customer = new ConsignmentCustomerDocument(event.customerId(), customerName);

        var sellers = event.salespersonIds().stream()
                .map(id -> new ConsignmentSellerDocument(id, resolveDisplayName(id)))
                .toList();

        var items = event.items().stream()
                .map(this::buildItemDocument)
                .toList();

        var doc = new ConsignmentDocument(
                event.consignmentId(),
                "OPEN",
                null,
                null,
                customerName,
                customer,
                sellers,
                items,
                Instant.now(clock));

        try {
            esClient.index(i -> i
                    .index(ElasticsearchIndexInitializer.INDEX_CONSIGNMENTS)
                    .id(event.consignmentId().toString())
                    .document(doc)
                    .refresh(Refresh.True));
            log.info("Indexed consignment {}", event.consignmentId());
        } catch (IOException e) {
            throw new IndexingException("Failed to index consignment " + event.consignmentId(), e);
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

    private ConsignmentItemDocument buildItemDocument(ConsignmentItemSnapshot item) {
        try {
            var productResponse = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_PRODUCTS).id(item.productId().toString()),
                    ProductDocument.class);
            var product = (productResponse.found() && productResponse.source() != null)
                    ? productResponse.source() : null;

            String productDescription = product != null ? product.description() : null;
            String brandDescription   = (product != null && product.brand() != null) ? product.brand().description() : null;
            String colorDescription   = (product != null && product.color() != null) ? product.color().description() : null;
            String sizeDescription    = null;
            if (product != null) {
                sizeDescription = product.skus().stream()
                        .filter(s -> s.id().equals(item.skuId()))
                        .findFirst()
                        .map(ProductSkuDocument::sizeDescription)
                        .orElse(null);
            }

            return new ConsignmentItemDocument(
                    item.skuId(), item.productId(),
                    productDescription, brandDescription, colorDescription, sizeDescription,
                    item.quantity(), 0, 0,
                    item.unitPrice());
        } catch (IOException e) {
            throw new IndexingException("Failed to enrich consignment item for SKU " + item.skuId(), e);
        }
    }
}
