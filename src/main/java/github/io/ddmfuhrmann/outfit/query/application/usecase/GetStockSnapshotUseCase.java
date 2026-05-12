package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockSnapshotDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetStockSnapshotUseCase {

    private final ElasticsearchClient esClient;

    public GetStockSnapshotUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public StockSnapshotDocument execute(Long skuId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.STOCK_SNAPSHOT_INDEX).id(skuId.toString()),
                    StockSnapshotDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("stock snapshot not found for SKU: " + skuId);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for stock snapshot: " + skuId, e);
        }
    }
}
