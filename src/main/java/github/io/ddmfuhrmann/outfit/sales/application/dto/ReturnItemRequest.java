package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReturnItemRequest(
        @NotNull Long skuId,
        @Positive int quantityReturned
) {}
