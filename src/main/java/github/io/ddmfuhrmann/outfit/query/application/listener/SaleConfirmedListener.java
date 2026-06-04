package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.query.application.usecase.IndexSaleUseCase;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleConfirmed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component("querySaleConfirmedListener")
public class SaleConfirmedListener {

    private final IndexSaleUseCase indexSale;

    public SaleConfirmedListener(IndexSaleUseCase indexSale) {
        this.indexSale = indexSale;
    }

    @ApplicationModuleListener
    public void on(SaleConfirmed event) {
        indexSale.execute(event);
    }
}
