package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.*;
import github.io.ddmfuhrmann.outfit.query.application.usecase.IndexProductUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ProductIndexListener {

    private final IndexProductUseCase indexProduct;

    public ProductIndexListener(IndexProductUseCase indexProduct) {
        this.indexProduct = indexProduct;
    }

    @ApplicationModuleListener
    public void on(ProductCreated event) {
        indexProduct.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(ProductUpdated event) {
        indexProduct.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(ProductDeactivated event) {
        indexProduct.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(ProductSkuCreated event) {
        indexProduct.execute(event.snapshot());
    }

    @ApplicationModuleListener
    public void on(ProductSkuDeactivated event) {
        indexProduct.execute(event.snapshot());
    }
}
