package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.ConsignmentItem;

import java.math.BigDecimal;

public record ConsignmentItemResponse(
        Long id,
        Long skuId,
        Long productId,
        BigDecimal unitPrice,
        int quantityIssued,
        int quantityReturned,
        int quantitySold
) {
    public static ConsignmentItemResponse from(ConsignmentItem item) {
        return new ConsignmentItemResponse(
                item.getId(),
                item.getSkuId(),
                item.getProductId(),
                item.getUnitPrice(),
                item.getQuantityIssued(),
                item.getQuantityReturned(),
                item.getQuantitySold()
        );
    }
}
