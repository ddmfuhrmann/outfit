package github.io.ddmfuhrmann.outfit.catalog.domain.event;

public record ProductUpdated(Long productId, ProductSnapshot snapshot) {}
