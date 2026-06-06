package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayableCancelled;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayableCreated;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayablePaid;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayablePaymentRecorded;
import github.io.ddmfuhrmann.outfit.query.application.dto.PayableDocument;
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
public class IndexPayableUseCase {

    private final ElasticsearchClient esClient;
    private final Clock clock;

    public IndexPayableUseCase(ElasticsearchClient esClient, Clock clock) {
        this.esClient = esClient;
        this.clock = clock;
    }

    public void indexCreated(PayableCreated event) {
        var doc = new PayableDocument(
                event.payableId(),
                event.purchaseId(),
                event.supplierId(),
                event.dueDate(),
                event.amount(),
                event.amount(),
                "OPEN",
                Instant.now(clock));

        try {
            esClient.index(i -> i
                    .index(ElasticsearchIndexInitializer.INDEX_PAYABLES)
                    .id(event.payableId().toString())
                    .document(doc)
                    .refresh(Refresh.True));
            log.info("Indexed payable {}", event.payableId());
        } catch (IOException e) {
            throw new IndexingException("Failed to index payable " + event.payableId(), e);
        }
    }

    public void updateBalanceAndStatus(PayablePaymentRecorded event) {
        try {
            esClient.update(u -> u
                            .index(ElasticsearchIndexInitializer.INDEX_PAYABLES)
                            .id(event.payableId().toString())
                            .doc(Map.<String, Object>of("balance", event.newBalance(), "status", event.newStatus()))
                            .refresh(Refresh.True),
                    PayableDocument.class);
            log.info("Updated payable {} balance/status in index", event.payableId());
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() == 404) {
                log.warn("Payable {} not found in index when updating balance/status", event.payableId());
                return;
            }
            throw new IndexingException("Failed to update payable " + event.payableId() + " balance/status", e);
        } catch (IOException e) {
            throw new IndexingException("Failed to update payable " + event.payableId() + " balance/status", e);
        }
    }

    public void markPaid(PayablePaid event) {
        try {
            esClient.update(u -> u
                            .index(ElasticsearchIndexInitializer.INDEX_PAYABLES)
                            .id(event.payableId().toString())
                            .doc(Map.<String, Object>of("balance", BigDecimal.ZERO, "status", "PAID"))
                            .refresh(Refresh.True),
                    PayableDocument.class);
            log.info("Marked payable {} as PAID in index", event.payableId());
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() == 404) {
                log.warn("Payable {} not found in index when marking PAID", event.payableId());
                return;
            }
            throw new IndexingException("Failed to mark payable " + event.payableId() + " as PAID", e);
        } catch (IOException e) {
            throw new IndexingException("Failed to mark payable " + event.payableId() + " as PAID", e);
        }
    }

    public void markCancelled(PayableCancelled event) {
        updateStatus(event.payableId(), "CANCELLED");
        log.info("Marked payable {} as CANCELLED in index", event.payableId());
    }

    private void updateStatus(Long payableId, String status) {
        try {
            esClient.update(u -> u
                            .index(ElasticsearchIndexInitializer.INDEX_PAYABLES)
                            .id(payableId.toString())
                            .doc(Map.of("status", status))
                            .refresh(Refresh.True),
                    PayableDocument.class);
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            if (e.status() == 404) {
                log.warn("Payable {} not found in index when marking {}", payableId, status);
                return;
            }
            throw new IndexingException("Failed to update payable " + payableId + " status to " + status, e);
        } catch (IOException e) {
            throw new IndexingException("Failed to update payable " + payableId + " status to " + status, e);
        }
    }
}
