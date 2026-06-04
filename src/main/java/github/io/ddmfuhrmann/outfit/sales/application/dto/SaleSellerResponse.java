package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleSeller;

import java.math.BigDecimal;

public record SaleSellerResponse(
        Long salespersonId,
        BigDecimal sharePercent) {

    public static SaleSellerResponse from(SaleSeller seller) {
        return new SaleSellerResponse(
                seller.getSalespersonId(),
                seller.getSharePercent());
    }
}
