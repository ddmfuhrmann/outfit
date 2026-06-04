package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateCommissionBonusTierRequest(
        @NotNull @PositiveOrZero BigDecimal minAmount,
        @NotNull @Positive BigDecimal maxAmount,
        @NotNull @PositiveOrZero BigDecimal bonusPercent) {}
