package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.PayableDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetPayableQueryUseCase {

    private final ElasticsearchClient esClient;

    public GetPayableQueryUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PayableDocument execute(Long payableId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_PAYABLES).id(payableId.toString()),
                    PayableDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("payable not found: " + payableId);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for payable: " + payableId, e);
        }
    }
}
