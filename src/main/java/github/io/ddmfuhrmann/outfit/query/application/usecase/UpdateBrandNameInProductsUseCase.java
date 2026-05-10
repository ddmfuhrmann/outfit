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
public class UpdateBrandNameInProductsUseCase {

    private static final String INDEX = "products";

    private final ElasticsearchClient esClient;

    public UpdateBrandNameInProductsUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(Long brandId, String newDescription) {
        try {
            esClient.updateByQuery(u -> u
                    .index(INDEX)
                    .query(q -> q.term(t -> t.field("brand.id").value(brandId)))
                    .script(Script.of(s -> s
                            .lang("painless")
                            .source("ctx._source.brand.description = params.name")
                            .params(Map.of("name", JsonData.of(newDescription)))))
                    .refresh(true));
            log.info("Updated brand {} name in products index", brandId);
        } catch (IOException e) {
            throw new IndexingException("Failed to update brand name in products: " + brandId, e);
        }
    }
}
