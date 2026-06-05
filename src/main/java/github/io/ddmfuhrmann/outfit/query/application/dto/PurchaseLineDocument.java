package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;

public record PurchaseLineDocument(
        Long productSkuId,
        String productDescription,
        String brandDescription,
        int quantity,
        BigDecimal unitCost,
        BigDecimal totalCost) {}
