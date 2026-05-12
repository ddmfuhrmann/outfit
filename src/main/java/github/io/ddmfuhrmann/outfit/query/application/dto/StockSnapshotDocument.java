package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.time.Instant;

public record StockSnapshotDocument(
        Long skuId, String barcode, Long sizeId, String sizeDescription,
        Long productId, String productDescription, boolean active,
        Long brandId, String brandDescription,
        Long categoryId, String categoryDescription,
        Long colorId, String colorDescription,
        int currentBalance, Instant updatedAt) {}
