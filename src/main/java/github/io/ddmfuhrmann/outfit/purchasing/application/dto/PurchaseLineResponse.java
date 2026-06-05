package github.io.ddmfuhrmann.outfit.purchasing.application.dto;

import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseLine;

import java.math.BigDecimal;

public record PurchaseLineResponse(Long id, Long productSkuId, int quantity, BigDecimal unitCost) {

    public static PurchaseLineResponse from(PurchaseLine line) {
        return new PurchaseLineResponse(line.getId(), line.getProductSkuId(), line.getQuantity(), line.getUnitCost());
    }
}
