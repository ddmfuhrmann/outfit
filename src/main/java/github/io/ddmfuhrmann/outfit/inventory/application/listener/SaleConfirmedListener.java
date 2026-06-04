package github.io.ddmfuhrmann.outfit.inventory.application.listener;

import github.io.ddmfuhrmann.outfit.inventory.application.usecase.RecordSaleStockDecrementUseCase;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleConfirmed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component("inventorySaleConfirmedListener")
public class SaleConfirmedListener {

    private final RecordSaleStockDecrementUseCase recordSaleStockDecrement;

    public SaleConfirmedListener(RecordSaleStockDecrementUseCase recordSaleStockDecrement) {
        this.recordSaleStockDecrement = recordSaleStockDecrement;
    }

    @ApplicationModuleListener
    public void on(SaleConfirmed event) {
        recordSaleStockDecrement.execute(event);
    }
}
