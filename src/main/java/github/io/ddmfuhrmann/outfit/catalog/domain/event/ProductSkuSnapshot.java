package github.io.ddmfuhrmann.outfit.catalog.domain.event;

public record ProductSkuSnapshot(Long id, String barcode, Long sizeId, boolean active) {}
