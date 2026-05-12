package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockSnapshotDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class SearchStockSnapshotUseCase {

    private final ElasticsearchClient esClient;

    public SearchStockSnapshotUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PageResponse<StockSnapshotDocument> execute(Long productId, Long brandId, Long categoryId, Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int size = pageable.getPageSize();
        try {
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.STOCK_SNAPSHOT_INDEX)
                            .from(from).size(size)
                            .query(q -> buildQuery(q, productId, brandId, categoryId)),
                    StockSnapshotDocument.class);
            return toPage(response, pageable, size);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed for stock snapshots", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, Long productId, Long brandId, Long categoryId) {
        boolean hasFilters = productId != null || brandId != null || categoryId != null;
        if (!hasFilters) return qb.matchAll(m -> m);
        return qb.bool(b -> {
            if (productId != null) b.filter(f -> f.term(t -> t.field("productId").value(productId)));
            if (brandId != null)   b.filter(f -> f.term(t -> t.field("brandId").value(brandId)));
            if (categoryId != null) b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
            return b;
        });
    }

    private PageResponse<StockSnapshotDocument> toPage(SearchResponse<StockSnapshotDocument> response,
                                                        Pageable pageable, int size) {
        long total = response.hits().total() != null ? response.hits().total().value() : 0L;
        List<StockSnapshotDocument> docs = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponse<>(docs, pageable.getPageNumber(), size, total, totalPages);
    }
}
