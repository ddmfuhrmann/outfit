package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.json.JsonData;
import github.io.ddmfuhrmann.outfit.inventory.domain.event.StockEntryRecorded;
import github.io.ddmfuhrmann.outfit.query.application.dto.ProductDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockMonthlyDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class UpdateStockMonthlyUseCase {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String PAINLESS_SCRIPT =
            "if (params.qty > 0) { ctx._source.totalInbound += params.qty } " +
            "else { ctx._source.totalOutbound += params.qty } " +
            "ctx._source.closingBalance = params.closingBalance";

    private final ElasticsearchClient esClient;

    public UpdateStockMonthlyUseCase(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void execute(StockEntryRecorded event) {
        try {
            YearMonth yearMonth = YearMonth.from(event.occurredAt().atZone(ZoneOffset.UTC));
            String yearMonthStr = yearMonth.format(YEAR_MONTH_FORMAT);
            String documentId = event.skuId() + "-" + yearMonthStr;

            var existing = esClient.get(
                    g -> g.index(ElasticsearchIndexInitializer.STOCK_MONTHLY_INDEX).id(documentId),
                    StockMonthlyDocument.class);

            if (existing.found()) {
                scriptUpdate(documentId, event);
            } else {
                createDocument(documentId, yearMonthStr, event);
            }
            log.info("Updated stock monthly for SKU {} month {}", event.skuId(), yearMonthStr);
        } catch (IOException e) {
            throw new IndexingException("Failed to update stock monthly for SKU " + event.skuId(), e);
        }
    }

    private void createDocument(String documentId, String yearMonthStr, StockEntryRecorded event) throws IOException {
        Long brandId = null;
        Long categoryId = null;
        var productResponse = esClient.get(
                g -> g.index(ElasticsearchIndexInitializer.INDEX_PRODUCTS).id(event.productId().toString()),
                ProductDocument.class);
        if (productResponse.found() && productResponse.source() != null) {
            var product = productResponse.source();
            brandId    = product.brand()    != null ? product.brand().id()    : null;
            categoryId = product.category() != null ? product.category().id() : null;
        }

        int openingBalance = event.runningBalance() - event.quantity();
        int totalInbound   = Math.max(event.quantity(), 0);
        int totalOutbound  = Math.min(event.quantity(), 0);

        var doc = new StockMonthlyDocument(
                event.skuId(), event.productId(),
                brandId, categoryId,
                yearMonthStr,
                openingBalance, totalInbound, totalOutbound, event.runningBalance());

        esClient.index(i -> i
                .index(ElasticsearchIndexInitializer.STOCK_MONTHLY_INDEX)
                .id(documentId)
                .document(doc)
                .refresh(Refresh.True));
    }

    private void scriptUpdate(String documentId, StockEntryRecorded event) throws IOException {
        esClient.update(u -> u
                .index(ElasticsearchIndexInitializer.STOCK_MONTHLY_INDEX)
                .id(documentId)
                .script(Script.of(s -> s
                        .lang("painless")
                        .source(PAINLESS_SCRIPT)
                        .params(Map.of(
                                "qty",            JsonData.of(event.quantity()),
                                "closingBalance", JsonData.of(event.runningBalance())))))
                .refresh(Refresh.True),
                StockMonthlyDocument.class);
    }
}
