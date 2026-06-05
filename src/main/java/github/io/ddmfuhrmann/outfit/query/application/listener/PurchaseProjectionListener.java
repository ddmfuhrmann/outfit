package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseCancelled;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseConfirmed;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseOpened;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseUpdated;
import github.io.ddmfuhrmann.outfit.query.application.usecase.IndexPurchaseUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.MarkPurchaseCancelledUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseProjectionListener {

    private final IndexPurchaseUseCase indexPurchase;
    private final MarkPurchaseCancelledUseCase markCancelled;

    public PurchaseProjectionListener(IndexPurchaseUseCase indexPurchase,
                                      MarkPurchaseCancelledUseCase markCancelled) {
        this.indexPurchase = indexPurchase;
        this.markCancelled = markCancelled;
    }

    @ApplicationModuleListener
    public void on(PurchaseOpened event) {
        indexPurchase.execute(new IndexPurchaseUseCase.PurchaseIndexInput(
                event.purchaseId(), event.brandId(), event.supplierId(),
                event.purchaseDate(), event.observations(), "OPEN",
                List.of(), List.of()));
    }

    @ApplicationModuleListener
    public void on(PurchaseUpdated event) {
        indexPurchase.execute(new IndexPurchaseUseCase.PurchaseIndexInput(
                event.purchaseId(), event.brandId(), event.supplierId(),
                event.purchaseDate(), event.observations(), event.status(),
                event.lines(), event.payables()));
    }

    @ApplicationModuleListener
    public void on(PurchaseConfirmed event) {
        indexPurchase.execute(new IndexPurchaseUseCase.PurchaseIndexInput(
                event.purchaseId(), event.brandId(), event.supplierId(),
                event.purchaseDate(), event.observations(), "CONFIRMED",
                event.lines(), event.payables()));
    }

    @ApplicationModuleListener
    public void on(PurchaseCancelled event) {
        markCancelled.execute(event.purchaseId());
    }
}
