package github.io.ddmfuhrmann.outfit.catalog.domain.event;

public record ProductDeactivated(Long productId, ProductSnapshot snapshot) {}
