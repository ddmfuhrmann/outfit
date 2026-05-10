package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class SearchProductsUseCase {

    private static final String INDEX = "products";

    private final ElasticsearchClient esClient;

    public SearchProductsUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PageResponse<ProductDocument> execute(String q, Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int size = pageable.getPageSize();
        try {
            var response = esClient.search(
                    s -> s.index(INDEX).from(from).size(size).query(qb -> buildQuery(qb, q)),
                    ProductDocument.class);
            return toPage(response, pageable, size);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, String q) {
        if (q == null || q.isBlank()) return qb.matchAll(m -> m);
        return qb.multiMatch(mm -> mm.query(q));
    }

    private PageResponse<ProductDocument> toPage(SearchResponse<ProductDocument> response, Pageable pageable, int size) {
        long total = response.hits().total() != null ? response.hits().total().value() : 0L;
        List<ProductDocument> docs = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponse<>(docs, pageable.getPageNumber(), size, total, totalPages);
    }
}
