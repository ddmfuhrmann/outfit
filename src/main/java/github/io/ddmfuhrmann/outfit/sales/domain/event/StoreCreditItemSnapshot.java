package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.math.BigDecimal;

public record StoreCreditItemSnapshot(
        Long skuId,
        Long productId,
        int quantity,
        BigDecimal unitPrice) {}
