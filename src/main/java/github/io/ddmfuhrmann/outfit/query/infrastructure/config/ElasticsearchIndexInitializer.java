package github.io.ddmfuhrmann.outfit.query.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

// NOTE: If a persistent Docker ES already contains indices created with pure dynamic mapping,
// those indices must be deleted before the first run with this initializer — a field type change
// from `text` to `search_as_you_type` is not accepted by Elasticsearch on an existing index.
// In Testcontainers environments this is a non-issue since ES starts fresh each test run.
@Component
@Slf4j
public class ElasticsearchIndexInitializer {

    // --- index names ---
    public static final String INDEX_PRODUCTS       = "products";
    public static final String INDEX_BRANDS         = "brands";
    public static final String INDEX_COLORS         = "colors";
    public static final String INDEX_CATEGORIES     = "categories";
    public static final String INDEX_SIZES          = "sizes";
    public static final String INDEX_PARTIES        = "parties";
    public static final String STOCK_SNAPSHOT_INDEX = "stock_snapshot";
    public static final String STOCK_MONTHLY_INDEX  = "stock_monthly";
    public static final String INDEX_CONSIGNMENTS   = "consignments";

    // --- shared field names ---
    public static final String FIELD_SKU_ID      = "skuId";
    public static final String FIELD_PRODUCT_ID  = "productId";
    public static final String FIELD_BRAND_ID    = "brandId";
    public static final String FIELD_CATEGORY_ID = "categoryId";
    public static final String FIELD_COLOR_ID    = "colorId";
    public static final String FIELD_ACTIVE      = "active";

    // --- stock_snapshot field names ---
    public static final String FIELD_CURRENT_BALANCE = "currentBalance";
    public static final String FIELD_UPDATED_AT      = "updatedAt";

    // --- stock_monthly field names ---
    public static final String FIELD_YEAR_MONTH      = "yearMonth";
    public static final String FIELD_OPENING_BALANCE = "openingBalance";
    public static final String FIELD_TOTAL_INBOUND   = "totalInbound";
    public static final String FIELD_TOTAL_OUTBOUND  = "totalOutbound";
    public static final String FIELD_CLOSING_BALANCE = "closingBalance";

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
                createIndex(name, m -> m.dynamic(DynamicMapping.True)
                        .properties(FIELD_DESCRIPTION, p -> p.searchAsYouType(s -> s)));
            }
            createIndex(INDEX_PARTIES, m -> m.dynamic(DynamicMapping.True)
                    .properties(FIELD_NAME,       p -> p.searchAsYouType(s -> s))
                    .properties(FIELD_LEGAL_NAME, p -> p.searchAsYouType(s -> s)));
            createIndex(STOCK_SNAPSHOT_INDEX, m -> m.dynamic(DynamicMapping.True)
                    .properties(FIELD_SKU_ID,          p -> p.keyword(k -> k))
                    .properties(FIELD_PRODUCT_ID,      p -> p.keyword(k -> k))
                    .properties(FIELD_BRAND_ID,        p -> p.keyword(k -> k))
                    .properties(FIELD_CATEGORY_ID,     p -> p.keyword(k -> k))
                    .properties(FIELD_COLOR_ID,        p -> p.keyword(k -> k))
                    .properties(FIELD_ACTIVE,          p -> p.boolean_(b -> b))
                    .properties(FIELD_CURRENT_BALANCE, p -> p.integer(i -> i))
                    .properties(FIELD_UPDATED_AT,      p -> p.date(d -> d)));
            createIndex(INDEX_CONSIGNMENTS, m -> m.dynamic(DynamicMapping.True)
                    .properties("customerName", p -> p.searchAsYouType(s -> s)));
            createIndex(STOCK_MONTHLY_INDEX, m -> m.dynamic(DynamicMapping.True)
                    .properties(FIELD_SKU_ID,          p -> p.keyword(k -> k))
                    .properties(FIELD_PRODUCT_ID,      p -> p.keyword(k -> k))
                    .properties(FIELD_BRAND_ID,        p -> p.keyword(k -> k))
                    .properties(FIELD_CATEGORY_ID,     p -> p.keyword(k -> k))
                    .properties(FIELD_YEAR_MONTH,      p -> p.keyword(k -> k))
                    .properties(FIELD_OPENING_BALANCE, p -> p.integer(i -> i))
                    .properties(FIELD_TOTAL_INBOUND,   p -> p.integer(i -> i))
                    .properties(FIELD_TOTAL_OUTBOUND,  p -> p.integer(i -> i))
                    .properties(FIELD_CLOSING_BALANCE, p -> p.integer(i -> i)));
        } catch (IOException e) {
            throw new IndexingException("Failed to initialize Elasticsearch indices", e);
        }
    }

    private void createIndex(String name,
                              Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> mapping)
            throws IOException {
        if (esClient.indices().exists(r -> r.index(name)).value()) return;
        esClient.indices().create(c -> c.index(name).mappings(mapping));
        log.info("Created ES index: {}", name);
    }
}
