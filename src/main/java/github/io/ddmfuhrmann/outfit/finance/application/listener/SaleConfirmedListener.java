package github.io.ddmfuhrmann.outfit.finance.application.listener;

import github.io.ddmfuhrmann.outfit.finance.application.usecase.CreateReceivablesFromSaleUseCase;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleConfirmed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component("financeSaleConfirmedListener")
public class SaleConfirmedListener {

    private final CreateReceivablesFromSaleUseCase createReceivablesFromSale;

    public SaleConfirmedListener(CreateReceivablesFromSaleUseCase createReceivablesFromSale) {
        this.createReceivablesFromSale = createReceivablesFromSale;
    }

    @ApplicationModuleListener
    public void on(SaleConfirmed event) {
        createReceivablesFromSale.execute(event);
    }
}
