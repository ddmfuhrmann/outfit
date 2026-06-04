package github.io.ddmfuhrmann.outfit.inventory.application.listener;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.ProductSkuCreated;
import github.io.ddmfuhrmann.outfit.inventory.application.usecase.RecordInitialStockUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class ProductSkuCreatedListener {

    private final RecordInitialStockUseCase recordInitialStock;
    private final Clock clock;

    public ProductSkuCreatedListener(RecordInitialStockUseCase recordInitialStock, Clock clock) {
        this.recordInitialStock = recordInitialStock;
        this.clock = clock;
    }

    @ApplicationModuleListener
    public void on(ProductSkuCreated event) {
        recordInitialStock.execute(event.skuId(), event.productId(), event.implantationQty(), Instant.now(clock));
    }
}
