package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleConfirmed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class RecordSaleStockDecrementUseCase {

    private final StockMovementService stockMovementService;

    public RecordSaleStockDecrementUseCase(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    public void execute(SaleConfirmed event) {
        if ("CONSIGNMENT".equals(event.origin())) {
            return;
        }

        for (var item : event.items()) {
            stockMovementService.recordEntry(
                    item.skuId(), item.productId(),
                    -item.quantity(),
                    StockSource.SALE, event.saleId(),
                    Instant.now());
        }
    }
}
