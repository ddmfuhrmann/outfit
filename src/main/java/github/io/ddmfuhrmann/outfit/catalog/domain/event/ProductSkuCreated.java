package github.io.ddmfuhrmann.outfit.catalog.domain.event;

public record ProductSkuCreated(Long skuId, Long productId, Long sizeId, String barcode, int implantationQty, ProductSnapshot snapshot) {}
