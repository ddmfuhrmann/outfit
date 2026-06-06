package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivableCreated;
import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivablePaid;
import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivablePaymentRecorded;
import github.io.ddmfuhrmann.outfit.query.application.dto.ReceivableDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.query.infrastructure.config.ElasticsearchIndexInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class IndexReceivableUseCase {

    private final ElasticsearchClient esClient;
    private final Clock clock;

    public IndexReceivableUseCase(ElasticsearchClient esClient, Clock clock) {
        this.esClient = esClient;
        this.clock = clock;
    }

    public void indexCreated(ReceivableCreated event) {
        var doc = new ReceivableDocument(
                event.receivableId(),
                event.saleId(),
                event.customerId(),
                event.dueDate(),
                event.amount(),
                event.amount(),
                "OPEN",
                Instant.now(clock));

        try {
            esClient.index(i -> i
                    .index(ElasticsearchIndexInitializer.INDEX_RECEIVABLES)
                    .id(event.receivableId().toString())
                    .document(doc)
                    .refresh(Refresh.True));
            log.info("Indexed receivable {}", event.receivableId());
        } catch (IOException e) {
            throw new IndexingException("Failed to index receivable " + event.receivableId(), e);
        }
    }

    public void updateBalanceAndStatus(ReceivablePaymentRecorded event) {
        try {
            esClient.update(u -> u
                            .index(ElasticsearchIndexInitializer.INDEX_RECEIVABLES)
                            .id(event.receivableId().toString())
                            .doc(Map.<String, Object>of("balance", event.newBalance(), "status", event.newStatus()))
                            .refresh(Refresh.True),
                    ReceivableDocument.class);
            log.info("Updated receivable {} balance/status in index", event.receivableId());
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() == 404) {
                log.warn("Receivable {} not found in index when updating balance/status", event.receivableId());
                return;
            }
            throw new IndexingException("Failed to update receivable " + event.receivableId() + " balance/status", e);
        } catch (IOException e) {
            throw new IndexingException("Failed to update receivable " + event.receivableId() + " balance/status", e);
        }
    }

    public void markPaid(ReceivablePaid event) {
        try {
            esClient.update(u -> u
                            .index(ElasticsearchIndexInitializer.INDEX_RECEIVABLES)
                            .id(event.receivableId().toString())
                            .doc(Map.<String, Object>of("balance", BigDecimal.ZERO, "status", "PAID"))
                            .refresh(Refresh.True),
                    ReceivableDocument.class);
            log.info("Marked receivable {} as PAID in index", event.receivableId());
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() == 404) {
                log.warn("Receivable {} not found in index when marking PAID", event.receivableId());
                return;
            }
            throw new IndexingException("Failed to mark receivable " + event.receivableId() + " as PAID", e);
        } catch (IOException e) {
            throw new IndexingException("Failed to mark receivable " + event.receivableId() + " as PAID", e);
        }
    }
}
