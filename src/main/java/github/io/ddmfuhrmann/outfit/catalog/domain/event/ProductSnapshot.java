package github.io.ddmfuhrmann.outfit.catalog.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProductSnapshot(
        Long id,
        String description,
        BigDecimal price,
        BigDecimal cost,
        LocalDate purchaseDate,
        Long colorId,
        Long brandId,
        Long categoryId,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<ProductSkuSnapshot> skus) {}
