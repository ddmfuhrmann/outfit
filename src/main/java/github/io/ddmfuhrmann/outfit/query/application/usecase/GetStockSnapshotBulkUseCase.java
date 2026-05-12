package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockSnapshotDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class GetStockSnapshotBulkUseCase {

    private final ElasticsearchClient esClient;

    public GetStockSnapshotBulkUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public List<StockSnapshotDocument> execute(List<Long> skuIds) {
        try {
            var ids = skuIds.stream().map(Object::toString).toList();
            var response = esClient.mget(m -> m
                            .index(ElasticsearchIndexInitializer.STOCK_SNAPSHOT_INDEX)
                            .ids(ids),
                    StockSnapshotDocument.class);
            return response.docs().stream()
                    .filter(hit -> hit.result().found())
                    .map(hit -> hit.result().source())
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch mget failed for stock snapshots", e);
        }
    }
}
