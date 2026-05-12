package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.inventory.domain.event.StockEntryRecorded;
import github.io.ddmfuhrmann.outfit.query.application.usecase.UpdateStockMonthlyUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.UpdateStockSnapshotUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StockEntryRecordedListener {

    private final UpdateStockSnapshotUseCase updateStockSnapshot;
    private final UpdateStockMonthlyUseCase updateStockMonthly;

    public StockEntryRecordedListener(UpdateStockSnapshotUseCase updateStockSnapshot,
                                      UpdateStockMonthlyUseCase updateStockMonthly) {
        this.updateStockSnapshot = updateStockSnapshot;
        this.updateStockMonthly = updateStockMonthly;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("stockProjectionExecutor")
    public void on(StockEntryRecorded event) {
        updateStockSnapshot.execute(event);
        updateStockMonthly.execute(event);
    }
}
