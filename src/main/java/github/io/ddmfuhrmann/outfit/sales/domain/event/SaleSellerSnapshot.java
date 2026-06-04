package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.math.BigDecimal;

public record SaleSellerSnapshot(
        Long salespersonId,
        BigDecimal sharePercent) {}
