package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockMonthlyDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class GetStockMonthlyUseCase {

    private final ElasticsearchClient esClient;

    public GetStockMonthlyUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PageResponse<StockMonthlyDocument> execute(Long skuId, Long productId, Long brandId,
                                                       Long categoryId, String yearMonth, Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int size = pageable.getPageSize();
        try {
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.STOCK_MONTHLY_INDEX)
                            .from(from).size(size)
                            .query(q -> buildQuery(q, skuId, productId, brandId, categoryId, yearMonth)),
                    StockMonthlyDocument.class);
            return toPage(response, pageable, size);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed for stock monthly", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, Long skuId, Long productId,
                                             Long brandId, Long categoryId, String yearMonth) {
        boolean hasFilters = skuId != null || productId != null || brandId != null
                || categoryId != null || yearMonth != null;
        if (!hasFilters) return qb.matchAll(m -> m);
        return qb.bool(b -> {
            if (skuId != null)      b.filter(f -> f.term(t -> t.field("skuId").value(skuId)));
            if (productId != null)  b.filter(f -> f.term(t -> t.field("productId").value(productId)));
            if (brandId != null)    b.filter(f -> f.term(t -> t.field("brandId").value(brandId)));
            if (categoryId != null) b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
            if (yearMonth != null)  b.filter(f -> f.term(t -> t.field("yearMonth").value(yearMonth)));
            return b;
        });
    }

    private PageResponse<StockMonthlyDocument> toPage(SearchResponse<StockMonthlyDocument> response,
                                                       Pageable pageable, int size) {
        long total = response.hits().total() != null ? response.hits().total().value() : 0L;
        List<StockMonthlyDocument> docs = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponse<>(docs, pageable.getPageNumber(), size, total, totalPages);
    }
}
