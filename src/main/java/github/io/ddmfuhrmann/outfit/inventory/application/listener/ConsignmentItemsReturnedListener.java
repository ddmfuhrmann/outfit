package github.io.ddmfuhrmann.outfit.inventory.application.listener;

import github.io.ddmfuhrmann.outfit.inventory.application.usecase.RecordConsignmentReturnStockUseCase;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemsReturned;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ConsignmentItemsReturnedListener {

    private final RecordConsignmentReturnStockUseCase recordConsignmentReturnStock;

    public ConsignmentItemsReturnedListener(RecordConsignmentReturnStockUseCase recordConsignmentReturnStock) {
        this.recordConsignmentReturnStock = recordConsignmentReturnStock;
    }

    @ApplicationModuleListener
    public void on(ConsignmentItemsReturned event) {
        recordConsignmentReturnStock.execute(event.consignmentId(), event.returnedItems());
    }
}
