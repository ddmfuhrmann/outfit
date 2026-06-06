package github.io.ddmfuhrmann.outfit.finance.application.listener;

import github.io.ddmfuhrmann.outfit.finance.application.usecase.CreatePayablesFromPurchaseUseCase;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseConfirmed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component("financePurchaseConfirmedListener")
public class PurchaseConfirmedListener {

    private final CreatePayablesFromPurchaseUseCase createPayablesFromPurchase;

    public PurchaseConfirmedListener(CreatePayablesFromPurchaseUseCase createPayablesFromPurchase) {
        this.createPayablesFromPurchase = createPayablesFromPurchase;
    }

    @ApplicationModuleListener
    public void on(PurchaseConfirmed event) {
        createPayablesFromPurchase.execute(event);
    }
}
