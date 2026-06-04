package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.math.BigDecimal;

public record SaleItemSnapshot(
        Long skuId,
        Long productId,
        int quantity,
        BigDecimal unitPrice) {}
