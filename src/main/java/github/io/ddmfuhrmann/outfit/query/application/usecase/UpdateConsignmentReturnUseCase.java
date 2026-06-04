package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.query.application.dto.ConsignmentDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.ConsignmentItemDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemSnapshot;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemsReturned;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UpdateConsignmentReturnUseCase {

    private final ElasticsearchClient esClient;
    private final Clock clock;

    public UpdateConsignmentReturnUseCase(ElasticsearchClient esClient, Clock clock) {
        this.esClient = esClient;
        this.clock = clock;
    }

    public void execute(ConsignmentItemsReturned event) {
        try {
            var existing = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_CONSIGNMENTS)
                            .id(event.consignmentId().toString()),
                    ConsignmentDocument.class);

            if (!existing.found() || existing.source() == null) return;
            var doc = existing.source();

            var returnedBySkuId = event.returnedItems().stream()
                    .collect(Collectors.toMap(ConsignmentItemSnapshot::skuId, ConsignmentItemSnapshot::quantity));

            var updatedItems = doc.items().stream()
                    .map(item -> {
                        int returned = returnedBySkuId.getOrDefault(item.skuId(), 0);
                        if (returned == 0) return item;
                        int newReturned = item.quantityReturned() + returned;
                        int newSold = item.quantityIssued() - newReturned;
                        return new ConsignmentItemDocument(
                                item.skuId(), item.productId(),
                                item.productDescription(), item.brandDescription(),
                                item.colorDescription(), item.sizeDescription(),
                                item.quantityIssued(), newReturned, newSold,
                                item.unitPrice());
                    })
                    .toList();

            var updated = new ConsignmentDocument(
                    doc.consignmentId(), doc.status(), doc.issueDate(), doc.closedAt(),
                    doc.customerName(), doc.customer(), doc.sellers(), updatedItems,
                    Instant.now(clock));

            esClient.index(i -> i
                    .index(ElasticsearchIndexInitializer.INDEX_CONSIGNMENTS)
                    .id(event.consignmentId().toString())
                    .document(updated)
                    .refresh(Refresh.True));

            log.info("Updated return quantities for consignment {}", event.consignmentId());
        } catch (IOException e) {
            throw new IndexingException("Failed to update consignment return " + event.consignmentId(), e);
        }
    }
}
