package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import github.io.ddmfuhrmann.outfit.query.application.dto.SaleDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;


@Service
public class SearchSalesUseCase {

    private final ElasticsearchClient esClient;

    public SearchSalesUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public record SearchSalesQuery(
            String q,
            Long customerId,
            Long salespersonId,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {}

    public PageResponse<SaleDocument> execute(SearchSalesQuery query) {
        int from = query.page() * query.size();
        try {
            var response = esClient.search(
                    s -> s.index(ElasticsearchIndexInitializer.INDEX_SALES)
                            .from(from)
                            .size(query.size())
                            .query(qb -> buildQuery(qb, query)),
                    SaleDocument.class);

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            List<SaleDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;
            return new PageResponse<>(docs, query.page(), query.size(), total, totalPages);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed for sales", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, SearchSalesQuery query) {
        boolean hasQ        = query.q() != null && !query.q().isBlank();
        boolean hasCustomer = query.customerId() != null;
        boolean hasSeller   = query.salespersonId() != null;
        boolean hasDateRange = query.from() != null || query.to() != null;

        if (!hasQ && !hasCustomer && !hasSeller && !hasDateRange) {
            return qb.matchAll(m -> m);
        }

        return qb.bool(b -> {
            if (hasQ)        b.must(textSearchQuery(query.q()));
            if (hasCustomer) b.filter(customerIdFilter(query.customerId()));
            if (hasSeller)   b.filter(sellerIdFilter(query.salespersonId()));
            if (hasDateRange) b.filter(issueDateRangeFilter(query.from(), query.to()));
            return b;
        });
    }

    private Query textSearchQuery(String q) {
        return Query.of(m -> m.multiMatch(mm -> mm
                .query(q)
                .type(TextQueryType.BoolPrefix)
                .fields("customerName", "customerName._2gram",
                        "customerName._3gram", "customerName._index_prefix")));
    }

    private Query customerIdFilter(Long customerId) {
        return Query.of(f -> f.term(t -> t.field("customerId").value(customerId)));
    }

    private Query sellerIdFilter(Long salespersonId) {
        return Query.of(f -> f.term(t -> t.field("sellers.id").value(salespersonId)));
    }

    private Query issueDateRangeFilter(LocalDate from, LocalDate to) {
        return Query.of(f -> f.range(r -> r.date(d -> {
            d.field("issueDate");
            if (from != null) d.gte(from.toString());
            if (to != null)   d.lte(to.toString());
            return d;
        })));
    }
}
