package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.sales.domain.event.StoreCreditNoteCreated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
public class RecordCreditReturnStockUseCase {

    private final StockMovementService stockMovementService;
    private final Clock clock;

    public RecordCreditReturnStockUseCase(StockMovementService stockMovementService, Clock clock) {
        this.stockMovementService = stockMovementService;
        this.clock = clock;
    }

    public void execute(StoreCreditNoteCreated event) {
        Instant now = Instant.now(clock);
        for (var item : event.items()) {
            stockMovementService.recordEntry(
                    item.skuId(), item.productId(),
                    item.quantity(),
                    StockSource.RETURN, event.storeCreditNoteId(),
                    now);
        }
    }
}
