package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.ConsignmentDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class SearchConsignmentsUseCase {

    private final ElasticsearchClient esClient;

    public SearchConsignmentsUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public record SearchConsignmentsQuery(
            String q,
            Long customerId,
            Long salespersonId,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {}

    public PageResponse<ConsignmentDocument> execute(SearchConsignmentsQuery query) {
        int from = query.page() * query.size();
        try {
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.INDEX_CONSIGNMENTS)
                            .from(from)
                            .size(query.size())
                            .query(qb -> buildQuery(qb, query)),
                    ConsignmentDocument.class);

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            List<ConsignmentDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;
            return new PageResponse<>(docs, query.page(), query.size(), total, totalPages);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed for consignments", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, SearchConsignmentsQuery query) {
        boolean hasQ          = query.q() != null && !query.q().isBlank();
        boolean hasCustomer   = query.customerId() != null;
        boolean hasSeller     = query.salespersonId() != null;
        boolean hasStatus     = query.status() != null && !query.status().isBlank();
        boolean hasFrom       = query.from() != null;
        boolean hasTo         = query.to() != null;

        if (!hasQ && !hasCustomer && !hasSeller && !hasStatus && !hasFrom && !hasTo) {
            return qb.matchAll(m -> m);
        }

        return qb.bool(b -> {
            if (hasQ) b.must(m -> m.multiMatch(mm -> mm
                    .query(query.q())
                    .type(TextQueryType.BoolPrefix)
                    .fields("customerName", "customerName._2gram",
                            "customerName._3gram", "customerName._index_prefix")));
            if (hasCustomer)
                b.filter(f -> f.term(t -> t.field("customer.id").value(query.customerId())));
            if (hasSeller)
                b.filter(f -> f.term(t -> t.field("sellers.id").value(query.salespersonId())));
            if (hasStatus)
                b.filter(f -> f.term(t -> t.field("status.keyword").value(query.status())));
            if (hasFrom || hasTo) {
                b.filter(f -> f.range(r -> r.date(d -> {
                    d.field("issueDate");
                    if (hasFrom) d.gte(query.from().toString());
                    if (hasTo)   d.lte(query.to().toString());
                    return d;
                })));
            }
            return b;
        });
    }
}
