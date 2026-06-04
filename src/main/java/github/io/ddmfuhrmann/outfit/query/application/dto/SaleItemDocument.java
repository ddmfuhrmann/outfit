package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;

public record SaleItemDocument(
        Long skuId,
        Long productId,
        String productDescription,
        String brandDescription,
        String colorDescription,
        String sizeDescription,
        int quantity,
        BigDecimal unitPrice) {}
