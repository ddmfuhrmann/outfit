package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetPurchaseQueryUseCase {

    private final ElasticsearchClient esClient;

    public GetPurchaseQueryUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PurchaseDocument execute(Long purchaseId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_PURCHASES)
                            .id(purchaseId.toString()),
                    PurchaseDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("purchase not found: " + purchaseId);
            }
            if (response.source() == null) {
                throw new ResourceNotFoundException("purchase not found: " + purchaseId);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for purchase: " + purchaseId, e);
        }
    }
}
