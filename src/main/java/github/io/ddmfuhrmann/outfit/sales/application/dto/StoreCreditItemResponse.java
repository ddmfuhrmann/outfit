package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditItem;

import java.math.BigDecimal;

public record StoreCreditItemResponse(
        Long id,
        Long skuId,
        Long productId,
        int quantity,
        BigDecimal unitPrice) {

    public static StoreCreditItemResponse from(StoreCreditItem item) {
        return new StoreCreditItemResponse(
                item.getId(),
                item.getSkuId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice());
    }
}
