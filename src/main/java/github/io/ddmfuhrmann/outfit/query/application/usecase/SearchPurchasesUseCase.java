package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class SearchPurchasesUseCase {

    private final ElasticsearchClient esClient;

    public SearchPurchasesUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public record SearchPurchasesQuery(
            Long brandId,
            Long supplierId,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {}

    public PageResponse<PurchaseDocument> execute(SearchPurchasesQuery query) {
        int from = query.page() * query.size();
        try {
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.INDEX_PURCHASES)
                            .from(from)
                            .size(query.size())
                            .query(qb -> buildQuery(qb, query)),
                    PurchaseDocument.class);

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            List<PurchaseDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;
            return new PageResponse<>(docs, query.page(), query.size(), total, totalPages);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed for purchases", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, SearchPurchasesQuery query) {
        boolean hasBrand     = query.brandId() != null;
        boolean hasSupplier  = query.supplierId() != null;
        boolean hasStatus    = query.status() != null && !query.status().isBlank();
        boolean hasDateRange = query.from() != null || query.to() != null;

        if (!hasBrand && !hasSupplier && !hasStatus && !hasDateRange) {
            return qb.matchAll(m -> m);
        }

        return qb.bool(b -> {
            if (hasBrand)    b.filter(Query.of(f -> f.term(t -> t.field("brandId").value(query.brandId()))));
            if (hasSupplier) b.filter(Query.of(f -> f.term(t -> t.field("supplierId").value(query.supplierId()))));
            if (hasStatus)   b.filter(Query.of(f -> f.term(t -> t.field("status").value(query.status()))));
            if (hasDateRange) b.filter(dateRangeFilter(query.from(), query.to()));
            return b;
        });
    }

    private Query dateRangeFilter(LocalDate from, LocalDate to) {
        return Query.of(f -> f.range(r -> r.date(d -> {
            d.field("purchaseDate");
            if (from != null) d.gte(from.toString());
            if (to != null)   d.lte(to.toString());
            return d;
        })));
    }
}
