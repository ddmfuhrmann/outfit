package github.io.ddmfuhrmann.outfit.sales.application.listener;

import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivablePaid;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.ActivateCommissionUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ReceivablePaidListener {

    private final ActivateCommissionUseCase activateCommission;

    public ReceivablePaidListener(ActivateCommissionUseCase activateCommission) {
        this.activateCommission = activateCommission;
    }

    @ApplicationModuleListener
    public void on(ReceivablePaid event) {
        activateCommission.execute(event.saleId(), event.receivableAmount(), event.saleTotalDeferredAmount());
    }
}
