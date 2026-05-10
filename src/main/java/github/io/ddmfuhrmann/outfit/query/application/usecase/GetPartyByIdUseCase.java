package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.PartyDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetPartyByIdUseCase {

    private static final String INDEX = "parties";

    private final ElasticsearchClient esClient;

    public GetPartyByIdUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PartyDocument execute(Long id) {
        try {
            var response = esClient.get(g -> g.index(INDEX).id(id.toString()), PartyDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("party not found: " + id);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for party: " + id, e);
        }
    }
}
