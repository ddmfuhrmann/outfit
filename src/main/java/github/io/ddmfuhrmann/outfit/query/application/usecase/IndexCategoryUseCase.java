package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.query.application.dto.RefDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class IndexCategoryUseCase {

    private static final String INDEX = "categories";

    private final ElasticsearchClient esClient;

    public IndexCategoryUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(Long id, String description) {
        try {
            esClient.index(i -> i
                    .index(INDEX)
                    .id(id.toString())
                    .document(new RefDocument(id, description))
                    .refresh(Refresh.True));
            log.info("Indexed category {}", id);
        } catch (IOException e) {
            throw new IndexingException("Failed to index category " + id, e);
        }
    }
}
