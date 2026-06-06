package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.ReceivableDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetReceivableQueryUseCase {

    private final ElasticsearchClient esClient;

    public GetReceivableQueryUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public ReceivableDocument execute(Long receivableId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_RECEIVABLES).id(receivableId.toString()),
                    ReceivableDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("receivable not found: " + receivableId);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for receivable: " + receivableId, e);
        }
    }
}
