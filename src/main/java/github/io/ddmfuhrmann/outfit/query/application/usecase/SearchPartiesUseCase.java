package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.util.ObjectBuilder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import github.io.ddmfuhrmann.outfit.query.application.dto.PartyDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class SearchPartiesUseCase {

    private static final String INDEX = "parties";

    private final ElasticsearchClient esClient;

    public SearchPartiesUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public PageResponse<PartyDocument> execute(String q, String role, Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int size = pageable.getPageSize();
        try {
            var response = esClient.search(
                    s -> s.index(INDEX).from(from).size(size).query(qb -> buildQuery(qb, q, role)),
                    PartyDocument.class);
            return toPage(response, pageable, size);
        } catch (IOException e) {
            throw new QueryException("Elasticsearch search failed", e);
        }
    }

    private ObjectBuilder<Query> buildQuery(Query.Builder qb, String q, String role) {
        boolean hasQ    = q    != null && !q.isBlank();
        boolean hasRole = role != null && !role.isBlank();
        if (!hasQ && !hasRole) return qb.matchAll(m -> m);
        return qb.bool(b -> {
            if (hasQ)    b.must(m -> m.multiMatch(mm -> mm
                    .query(q)
                    .type(TextQueryType.BoolPrefix)
                    .fields("name", "name._2gram", "name._3gram", "name._index_prefix",
                            "legalName", "legalName._2gram", "legalName._3gram", "legalName._index_prefix",
                            "cnpj", "cpf")
            ));
            if (hasRole) b.filter(f -> f.term(t -> t.field(role).value(true)));
            return b;
        });
    }

    private PageResponse<PartyDocument> toPage(SearchResponse<PartyDocument> response, Pageable pageable, int size) {
        long total = response.hits().total() != null ? response.hits().total().value() : 0L;
        List<PartyDocument> docs = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponse<>(docs, pageable.getPageNumber(), size, total, totalPages);
    }
}

