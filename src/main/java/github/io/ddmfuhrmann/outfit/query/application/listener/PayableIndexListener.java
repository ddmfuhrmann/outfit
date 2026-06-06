package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.finance.domain.event.PayableCancelled;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayableCreated;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayablePaid;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayablePaymentRecorded;
import github.io.ddmfuhrmann.outfit.query.application.usecase.IndexPayableUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class PayableIndexListener {

    private final IndexPayableUseCase indexPayable;

    public PayableIndexListener(IndexPayableUseCase indexPayable) {
        this.indexPayable = indexPayable;
    }

    @ApplicationModuleListener
    public void on(PayableCreated event) {
        indexPayable.indexCreated(event);
    }

    @ApplicationModuleListener
    public void on(PayablePaymentRecorded event) {
        indexPayable.updateBalanceAndStatus(event);
    }

    @ApplicationModuleListener
    public void on(PayablePaid event) {
        indexPayable.markPaid(event);
    }

    @ApplicationModuleListener
    public void on(PayableCancelled event) {
        indexPayable.markCancelled(event);
    }
}
