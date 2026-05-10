package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class DeleteColorFromIndexUseCase {

    private static final String INDEX = "colors";

    private final ElasticsearchClient esClient;

    public DeleteColorFromIndexUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(Long id) {
        try {
            esClient.delete(d -> d.index(INDEX).id(id.toString()).refresh(Refresh.True));
            log.info("Deleted color {} from index", id);
        } catch (IOException e) {
            throw new IndexingException("Failed to delete color from index: " + id, e);
        }
    }
}
