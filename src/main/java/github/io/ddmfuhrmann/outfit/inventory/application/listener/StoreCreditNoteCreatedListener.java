package github.io.ddmfuhrmann.outfit.inventory.application.listener;

import github.io.ddmfuhrmann.outfit.inventory.application.usecase.RecordCreditReturnStockUseCase;
import github.io.ddmfuhrmann.outfit.sales.domain.event.StoreCreditNoteCreated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class StoreCreditNoteCreatedListener {

    private final RecordCreditReturnStockUseCase recordCreditReturnStock;

    public StoreCreditNoteCreatedListener(RecordCreditReturnStockUseCase recordCreditReturnStock) {
        this.recordCreditReturnStock = recordCreditReturnStock;
    }

    @ApplicationModuleListener
    public void on(StoreCreditNoteCreated event) {
        recordCreditReturnStock.execute(event);
    }
}
