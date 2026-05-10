package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.json.JsonData;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class UpdateCategoryNameInProductsUseCase {

    private static final String INDEX = "products";

    private final ElasticsearchClient esClient;

    public UpdateCategoryNameInProductsUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(Long categoryId, String newDescription) {
        try {
            esClient.updateByQuery(u -> u
                    .index(INDEX)
                    .query(q -> q.term(t -> t.field("category.id").value(categoryId)))
                    .script(Script.of(s -> s
                            .lang("painless")
                            .source("ctx._source.category.description = params.name")
                            .params(Map.of("name", JsonData.of(newDescription)))))
                    .refresh(true));
            log.info("Updated category {} name in products index", categoryId);
        } catch (IOException e) {
            throw new IndexingException("Failed to update category name in products: " + categoryId, e);
        }
    }
}
