package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.query.application.dto.*;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleConfirmed;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleItemSnapshot;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleSellerSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Service
public class IndexSaleUseCase {

    private final ElasticsearchClient esClient;

    public IndexSaleUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(SaleConfirmed event) {
        String customerName = resolveDisplayName(event.customerId());
        var customer = new SaleCustomerDocument(event.customerId(), customerName);

        var sellers = event.sellers().stream()
                .map(s -> new SaleSellerDocument(s.salespersonId(), resolveDisplayName(s.salespersonId()), s.sharePercent()))
                .toList();

        var items = event.items().stream()
                .map(this::buildItemDocument)
                .toList();

        var installments = event.installments().stream()
                .map(i -> new SaleInstallmentDocument(i.paymentModality(), i.dueDate(), i.amount()))
                .toList();

        var doc = new SaleDocument(
                event.saleId(),
                event.customerId(),
                customerName,
                customer,
                event.origin(),
                event.consignmentId(),
                event.issueDate(),
                event.grossAmount(),
                event.storeCreditDiscount(),
                event.netAmount(),
                sellers,
                items,
                installments,
                Instant.now());

        try {
            esClient.index(i -> i
                    .index(ElasticsearchIndexInitializer.INDEX_SALES)
                    .id(event.saleId().toString())
                    .document(doc)
                    .refresh(Refresh.True));
            log.info("Indexed sale {}", event.saleId());
        } catch (IOException e) {
            throw new IndexingException("Failed to index sale " + event.saleId(), e);
        }
    }

    private String resolveDisplayName(Long partyId) {
        try {
            var response = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_PARTIES).id(partyId.toString()),
                    PartyDocument.class);
            if (!response.found() || response.source() == null) return null;
            var party = response.source();
            return (party.name() != null && !party.name().isBlank()) ? party.name() : party.legalName();
        } catch (IOException e) {
            throw new IndexingException("Failed to resolve party display name for " + partyId, e);
        }
    }

    private SaleItemDocument buildItemDocument(SaleItemSnapshot item) {
        try {
            var productResponse = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.INDEX_PRODUCTS).id(item.productId().toString()),
                    ProductDocument.class);
            var product = (productResponse.found() && productResponse.source() != null)
                    ? productResponse.source() : null;

            String productDescription = product != null ? product.description() : null;
            String brandDescription   = (product != null && product.brand() != null) ? product.brand().description() : null;
            String colorDescription   = (product != null && product.color() != null) ? product.color().description() : null;
            String sizeDescription    = null;
            if (product != null) {
                sizeDescription = product.skus().stream()
                        .filter(s -> s.id().equals(item.skuId()))
                        .findFirst()
                        .map(ProductSkuDocument::sizeDescription)
                        .orElse(null);
            }

            return new SaleItemDocument(
                    item.skuId(), item.productId(),
                    productDescription, brandDescription, colorDescription, sizeDescription,
                    item.quantity(), item.unitPrice());
        } catch (IOException e) {
            throw new IndexingException("Failed to enrich sale item for SKU " + item.skuId(), e);
        }
    }
}
