package github.io.ddmfuhrmann.outfit.purchasing.domain.event;

import java.math.BigDecimal;

public record PurchaseLineSnapshot(Long productSkuId, int quantity, BigDecimal unitCost) {}
