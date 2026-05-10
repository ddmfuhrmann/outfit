package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProductDocument(
        Long id,
        String description,
        BigDecimal price,
        BigDecimal cost,
        LocalDate purchaseDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        ProductRefDocument color,
        ProductRefDocument brand,
        ProductRefDocument category,
        List<ProductSkuDocument> skus) {}
