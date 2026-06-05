package github.io.ddmfuhrmann.outfit.purchasing.application.listener;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.ProductSkuCreated;
import github.io.ddmfuhrmann.outfit.purchasing.application.usecase.CreateOrUpdatePurchaseUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component("purchasingProductSkuCreatedListener")
public class ProductSkuCreatedListener {

    private final CreateOrUpdatePurchaseUseCase createOrUpdatePurchase;

    public ProductSkuCreatedListener(CreateOrUpdatePurchaseUseCase createOrUpdatePurchase) {
        this.createOrUpdatePurchase = createOrUpdatePurchase;
    }

    @ApplicationModuleListener
    public void on(ProductSkuCreated event) {
        var snapshot = event.snapshot();

        if (snapshot.purchaseDate() == null) {
            return;
        }

        Long supplierId;
        if (snapshot.supplierIds().size() == 1) {
            supplierId = snapshot.supplierIds().getFirst();
        } else if (snapshot.supplierIds().isEmpty()) {
            supplierId = null;
        } else {
            supplierId = null;
            log.warn("SKU {} has {} suppliers — supplierId not set on purchase", event.skuId(), snapshot.supplierIds().size());
        }

        String observations = "Compra " + snapshot.brandId() + " – " + snapshot.purchaseDate();

        createOrUpdatePurchase.execute(
                snapshot.brandId(),
                supplierId,
                snapshot.purchaseDate(),
                observations,
                event.skuId(),
                event.implantationQty(),
                snapshot.cost());
    }
}
