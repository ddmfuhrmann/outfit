package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.PayableDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class SearchPayablesUseCase {

    private final ElasticsearchClient esClient;

    public SearchPayablesUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public record SearchPayablesQuery(
            Long supplierId,
            String status,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            int page,
            int size) {}

    public PageResponse<PayableDocument> execute(SearchPayablesQuery query) {
        int from = query.page() * query.size();
        try {
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.INDEX_PAYABLES)
                            .from(from)
                            .size(query.size())
                            .query(qb -> buildQuery(qb, query)),
                    PayableDocument.class);

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            List<PayableDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;
            return new PageResponse<>(docs, query.page(), query.size(), total, totalPages);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed for payables", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, SearchPayablesQuery query) {
        boolean hasSupplier  = query.supplierId() != null;
        boolean hasStatus    = query.status() != null && !query.status().isBlank();
        boolean hasDateRange = query.dueDateFrom() != null || query.dueDateTo() != null;

        if (!hasSupplier && !hasStatus && !hasDateRange) {
            return qb.matchAll(m -> m);
        }

        return qb.bool(b -> {
            if (hasSupplier)  b.filter(Query.of(f -> f.term(t -> t.field("supplierId").value(query.supplierId()))));
            if (hasStatus)    b.filter(Query.of(f -> f.term(t -> t.field("status").value(query.status()))));
            if (hasDateRange) b.filter(dueDateRangeFilter(query.dueDateFrom(), query.dueDateTo()));
            return b;
        });
    }

    private Query dueDateRangeFilter(LocalDate from, LocalDate to) {
        return Query.of(f -> f.range(r -> r.date(d -> {
            d.field("dueDate");
            if (from != null) d.gte(from.toString());
            if (to != null)   d.lte(to.toString());
            return d;
        })));
    }
}
