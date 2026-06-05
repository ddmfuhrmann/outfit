package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class MarkPurchaseCancelledUseCase {

    private final ElasticsearchClient esClient;

    public MarkPurchaseCancelledUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(Long purchaseId) {
        try {
            esClient.update(u -> u
                            .index(ElasticsearchIndexInitializer.INDEX_PURCHASES)
                            .id(purchaseId.toString())
                            .doc(Map.of("status", "CANCELLED"))
                            .refresh(Refresh.True),
                    PurchaseDocument.class);
            log.info("Marked purchase {} as CANCELLED in index", purchaseId);
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() == 404) {
                log.warn("Purchase {} not found in index when marking CANCELLED", purchaseId);
                return;
            }
            throw new IndexingException("Failed to mark purchase " + purchaseId + " as CANCELLED", e);
        } catch (IOException e) {
            throw new IndexingException("Failed to mark purchase " + purchaseId + " as CANCELLED", e);
        }
    }
}
