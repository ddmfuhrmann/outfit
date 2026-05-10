package github.io.ddmfuhrmann.outfit.catalog.domain.event;

public record ProductCreated(Long productId, ProductSnapshot snapshot) {}
