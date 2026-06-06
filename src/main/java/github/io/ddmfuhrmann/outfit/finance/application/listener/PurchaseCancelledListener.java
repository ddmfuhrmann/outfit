package github.io.ddmfuhrmann.outfit.finance.application.listener;

import github.io.ddmfuhrmann.outfit.finance.application.usecase.CancelPayablesForPurchaseUseCase;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseCancelled;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component("financePurchaseCancelledListener")
public class PurchaseCancelledListener {

    private final CancelPayablesForPurchaseUseCase cancelPayablesForPurchase;

    public PurchaseCancelledListener(CancelPayablesForPurchaseUseCase cancelPayablesForPurchase) {
        this.cancelPayablesForPurchase = cancelPayablesForPurchase;
    }

    @ApplicationModuleListener
    public void on(PurchaseCancelled event) {
        cancelPayablesForPurchase.execute(event.purchaseId());
    }
}
