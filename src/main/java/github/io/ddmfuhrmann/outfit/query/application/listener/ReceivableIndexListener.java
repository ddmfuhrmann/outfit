package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivableCreated;
import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivablePaid;
import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivablePaymentRecorded;
import github.io.ddmfuhrmann.outfit.query.application.usecase.IndexReceivableUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ReceivableIndexListener {

    private final IndexReceivableUseCase indexReceivable;

    public ReceivableIndexListener(IndexReceivableUseCase indexReceivable) {
        this.indexReceivable = indexReceivable;
    }

    @ApplicationModuleListener
    public void on(ReceivableCreated event) {
        indexReceivable.indexCreated(event);
    }

    @ApplicationModuleListener
    public void on(ReceivablePaymentRecorded event) {
        indexReceivable.updateBalanceAndStatus(event);
    }

    @ApplicationModuleListener
    public void on(ReceivablePaid event) {
        indexReceivable.markPaid(event);
    }
}
