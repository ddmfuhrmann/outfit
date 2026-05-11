package github.io.ddmfuhrmann.outfit.query.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

// NOTE: If a persistent Docker ES already contains indices created with pure dynamic mapping,
// those indices must be deleted before the first run with this initializer — a field type change
// from `text` to `search_as_you_type` is not accepted by Elasticsearch on an existing index.
// In Testcontainers environments this is a non-issue since ES starts fresh each test run.
@Component
@Slf4j
class ElasticsearchIndexInitializer {

    static final String INDEX_PRODUCTS   = "products";
    static final String INDEX_BRANDS     = "brands";
    static final String INDEX_COLORS     = "colors";
    static final String INDEX_CATEGORIES = "categories";
    static final String INDEX_SIZES      = "sizes";
    static final String INDEX_PARTIES    = "parties";

    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_NAME        = "name";
    private static final String FIELD_LEGAL_NAME  = "legalName";

    private static final List<String> DESCRIPTION_INDICES =
            List.of(INDEX_PRODUCTS, INDEX_BRANDS, INDEX_COLORS, INDEX_CATEGORIES, INDEX_SIZES);

    private final ElasticsearchClient esClient;

    ElasticsearchIndexInitializer(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    void initIndices() {
        try {
            for (String name : DESCRIPTION_INDICES) {
                createDescriptionIndex(name);
            }
            createPartiesIndex();
        } catch (IOException e) {
            throw new IndexingException("Failed to initialize Elasticsearch indices", e);
        }
    }

    private void createDescriptionIndex(String name) throws IOException {
        if (esClient.indices().exists(r -> r.index(name)).value()) return;
        esClient.indices().create(c -> c
                .index(name)
                .mappings(m -> m
                        .dynamic(DynamicMapping.True)
                        .properties(FIELD_DESCRIPTION, p -> p.searchAsYouType(s -> s))
                )
        );
        log.info("Created ES index: {}", name);
    }

    private void createPartiesIndex() throws IOException {
        if (esClient.indices().exists(r -> r.index(INDEX_PARTIES)).value()) return;
        esClient.indices().create(c -> c
                .index(INDEX_PARTIES)
                .mappings(m -> m
                        .dynamic(DynamicMapping.True)
                        .properties(FIELD_NAME,       p -> p.searchAsYouType(s -> s))
                        .properties(FIELD_LEGAL_NAME, p -> p.searchAsYouType(s -> s))
                )
        );
        log.info("Created ES index: {}", INDEX_PARTIES);
    }
}
