package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSaleSellerRequest(
        @NotNull Long salespersonId,
        @NotNull BigDecimal sharePercent) {}
