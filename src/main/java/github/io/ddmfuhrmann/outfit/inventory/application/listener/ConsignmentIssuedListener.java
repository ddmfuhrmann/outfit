package github.io.ddmfuhrmann.outfit.inventory.application.listener;

import github.io.ddmfuhrmann.outfit.inventory.application.usecase.RecordConsignmentIssueStockUseCase;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentIssued;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ConsignmentIssuedListener {

    private final RecordConsignmentIssueStockUseCase recordConsignmentIssueStock;

    public ConsignmentIssuedListener(RecordConsignmentIssueStockUseCase recordConsignmentIssueStock) {
        this.recordConsignmentIssueStock = recordConsignmentIssueStock;
    }

    @ApplicationModuleListener
    public void on(ConsignmentIssued event) {
        recordConsignmentIssueStock.execute(event.consignmentId(), event.items());
    }
}
