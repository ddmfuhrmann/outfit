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
public class UpdateSizeNameInProductsUseCase {

    private static final String INDEX = "products";

    private final ElasticsearchClient esClient;

    public UpdateSizeNameInProductsUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(Long sizeId, String newDescription) {
        try {
            esClient.updateByQuery(u -> u
                    .index(INDEX)
                    .query(q -> q.term(t -> t.field("skus.sizeId").value(sizeId)))
                    .script(Script.of(s -> s
                            .lang("painless")
                            .source("for (sku in ctx._source.skus) { if (sku.sizeId == params.id) sku.sizeDescription = params.name; }")
                            .params(Map.of(
                                    "id", JsonData.of(sizeId),
                                    "name", JsonData.of(newDescription)))))
                    .refresh(true));
            log.info("Updated size {} name in products index", sizeId);
        } catch (IOException e) {
            throw new IndexingException("Failed to update size name in products: " + sizeId, e);
        }
    }
}
