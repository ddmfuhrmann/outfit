package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class RecordConsignmentIssueStockUseCase {

    private final StockMovementService stockMovementService;
    private final Clock clock;

    public RecordConsignmentIssueStockUseCase(StockMovementService stockMovementService, Clock clock) {
        this.stockMovementService = stockMovementService;
        this.clock = clock;
    }

    public void execute(Long consignmentId, List<ConsignmentItemSnapshot> items) {
        Instant now = Instant.now(clock);
        for (var item : items) {
            stockMovementService.recordEntry(
                    item.skuId(), item.productId(),
                    -item.quantity(),
                    StockSource.CONSIGNMENT, consignmentId,
                    now);
        }
    }
}
