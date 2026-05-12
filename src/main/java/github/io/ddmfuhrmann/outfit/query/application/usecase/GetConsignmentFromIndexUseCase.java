package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.ConsignmentDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetConsignmentFromIndexUseCase {

    private final ElasticsearchClient esClient;

    public GetConsignmentFromIndexUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public ConsignmentDocument execute(Long consignmentId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_CONSIGNMENTS)
                            .id(consignmentId.toString()),
                    ConsignmentDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("consignment not found: " + consignmentId);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for consignment: " + consignmentId, e);
        }
    }
}
