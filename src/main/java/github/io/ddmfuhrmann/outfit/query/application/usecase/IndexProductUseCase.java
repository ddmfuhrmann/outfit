package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.ProductSkuSnapshot;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.ProductSnapshot;
import github.io.ddmfuhrmann.outfit.query.application.dto.*;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class IndexProductUseCase {

    private static final String INDEX = "products";

    private final ElasticsearchClient esClient;

    public IndexProductUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(ProductSnapshot snapshot) {
        try {
            var doc = new ProductDocument(
                    snapshot.id(),
                    snapshot.description(),
                    snapshot.price(),
                    snapshot.cost(),
                    snapshot.purchaseDate(),
                    snapshot.active(),
                    snapshot.createdAt(),
                    snapshot.updatedAt(),
                    lookupRef("colors", snapshot.colorId()),
                    lookupRef("brands", snapshot.brandId()),
                    lookupRef("categories", snapshot.categoryId()),
                    snapshot.skus().stream().map(this::toSkuDocument).toList());

            esClient.index(i -> i
                    .index(INDEX)
                    .id(doc.id().toString())
                    .document(doc)
                    .refresh(Refresh.True));
            log.info("Indexed product {}", snapshot.id());
        } catch (IOException e) {
            throw new IndexingException("Failed to index product " + snapshot.id(), e);
        }
    }

    private ProductSkuDocument toSkuDocument(ProductSkuSnapshot s) {
        RefDocument size = lookupRefUnchecked("sizes", s.sizeId());
        return new ProductSkuDocument(
                s.id(), s.barcode(), s.sizeId(),
                size != null ? size.description() : null,
                s.active());
    }

    private ProductRefDocument lookupRef(String index, Long id) {
        if (id == null) return null;
        RefDocument ref = lookupRefUnchecked(index, id);
        return ref != null ? new ProductRefDocument(ref.id(), ref.description()) : new ProductRefDocument(id, null);
    }

    private RefDocument lookupRefUnchecked(String index, Long id) {
        try {
            var response = esClient.get(g -> g.index(index).id(id.toString()), RefDocument.class);
            return response.found() ? response.source() : null;
        } catch (IOException e) {
            throw new IndexingException("Failed to lookup " + index + "/" + id, e);
        }
    }
}
