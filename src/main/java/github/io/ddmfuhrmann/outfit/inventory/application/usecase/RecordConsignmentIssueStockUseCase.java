package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class RecordConsignmentIssueStockUseCase {

    private final StockMovementService stockMovementService;

    public RecordConsignmentIssueStockUseCase(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    public void execute(Long consignmentId, List<ConsignmentItemSnapshot> items) {
        for (var item : items) {
            stockMovementService.recordEntry(
                    item.skuId(), item.productId(),
                    -item.quantity(),
                    StockSource.CONSIGNMENT, consignmentId,
                    Instant.now()
            );
        }
    }
}
