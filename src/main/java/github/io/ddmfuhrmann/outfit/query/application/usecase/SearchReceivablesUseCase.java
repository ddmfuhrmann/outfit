package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.ReceivableDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class SearchReceivablesUseCase {

    private final ElasticsearchClient esClient;

    public SearchReceivablesUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public record SearchReceivablesQuery(
            Long customerId,
            String status,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            int page,
            int size) {}

    public PageResponse<ReceivableDocument> execute(SearchReceivablesQuery query) {
        int from = query.page() * query.size();
        try {
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.INDEX_RECEIVABLES)
                            .from(from)
                            .size(query.size())
                            .query(qb -> buildQuery(qb, query)),
                    ReceivableDocument.class);

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            List<ReceivableDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;
            return new PageResponse<>(docs, query.page(), query.size(), total, totalPages);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed for receivables", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, SearchReceivablesQuery query) {
        boolean hasCustomer  = query.customerId() != null;
        boolean hasStatus    = query.status() != null && !query.status().isBlank();
        boolean hasDateRange = query.dueDateFrom() != null || query.dueDateTo() != null;

        if (!hasCustomer && !hasStatus && !hasDateRange) {
            return qb.matchAll(m -> m);
        }

        return qb.bool(b -> {
            if (hasCustomer)  b.filter(Query.of(f -> f.term(t -> t.field("customerId").value(query.customerId()))));
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
