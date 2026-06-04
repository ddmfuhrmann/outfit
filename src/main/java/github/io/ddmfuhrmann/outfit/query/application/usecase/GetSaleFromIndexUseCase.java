package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import github.io.ddmfuhrmann.outfit.query.application.dto.SaleDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.QueryException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GetSaleFromIndexUseCase {

    private final ElasticsearchClient esClient;

    public GetSaleFromIndexUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public SaleDocument execute(Long saleId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_SALES).id(saleId.toString()),
                    SaleDocument.class);
            if (!response.found()) {
                throw new ResourceNotFoundException("sale not found: " + saleId);
            }
            return response.source();
        } catch (IOException e) {
            throw new QueryException("Elasticsearch get failed for sale: " + saleId, e);
        }
    }
}
