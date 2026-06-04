package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleConfirmed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
public class RecordSaleStockDecrementUseCase {

    private final StockMovementService stockMovementService;
    private final Clock clock;

    public RecordSaleStockDecrementUseCase(StockMovementService stockMovementService, Clock clock) {
        this.stockMovementService = stockMovementService;
        this.clock = clock;
    }

    public void execute(SaleConfirmed event) {
        if ("CONSIGNMENT".equals(event.origin())) {
            return;
        }

        Instant now = Instant.now(clock);
        for (var item : event.items()) {
            stockMovementService.recordEntry(
                    item.skuId(), item.productId(),
                    -item.quantity(),
                    StockSource.SALE, event.saleId(),
                    now);
        }
    }
}
