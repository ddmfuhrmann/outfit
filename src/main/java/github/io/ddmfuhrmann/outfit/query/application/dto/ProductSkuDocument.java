package github.io.ddmfuhrmann.outfit.query.application.dto;

public record ProductSkuDocument(Long id, String barcode, Long sizeId, String sizeDescription, boolean active) {}
