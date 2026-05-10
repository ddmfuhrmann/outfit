package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.ProductSku;

public record ProductSkuResponse(Long id, String barcode, Long sizeId, String sizeDescription, boolean active) {

    public static ProductSkuResponse from(ProductSku sku, String sizeDescription) {
        return new ProductSkuResponse(sku.getId(), sku.getBarcode(), sku.getSizeId(), sizeDescription, sku.isActive());
    }
}
