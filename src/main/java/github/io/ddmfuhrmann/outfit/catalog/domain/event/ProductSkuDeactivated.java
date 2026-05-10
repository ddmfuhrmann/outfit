package github.io.ddmfuhrmann.outfit.catalog.domain.event;

public record ProductSkuDeactivated(Long skuId, Long productId, ProductSnapshot snapshot) {}
