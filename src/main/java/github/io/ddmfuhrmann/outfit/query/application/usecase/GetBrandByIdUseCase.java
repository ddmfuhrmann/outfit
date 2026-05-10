package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.RefDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetBrandByIdUseCase {

    private static final String INDEX = "brands";

    private final ElasticsearchClient esClient;

    public GetBrandByIdUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public RefDocument execute(Long id) {
        try {
            boolean indexExists = esClient.indices().exists(e -> e.index(INDEX)).value();
            if (!indexExists) throw new ResourceNotFoundException("brand not found: " + id);
            var response = esClient.get(g -> g.index(INDEX).id(id.toString()), RefDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("brand not found: " + id);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for brand: " + id, e);
        }
    }
}
