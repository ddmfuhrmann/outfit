package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleItem;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long skuId,
        Long productId,
        int quantity,
        BigDecimal unitPrice) {

    public static SaleItemResponse from(SaleItem item) {
        return new SaleItemResponse(
                item.getId(),
                item.getSkuId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice());
    }
}
