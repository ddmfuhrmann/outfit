package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateSaleItemRequest(
        @NotNull Long skuId,
        @NotNull Long productId,
        @Positive int quantity,
        @NotNull @Positive BigDecimal unitPrice) {}
