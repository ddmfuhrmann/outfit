package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.CommissionBonusTier;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionBonusTierResponse(
        Long id,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal bonusPercent,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static CommissionBonusTierResponse from(CommissionBonusTier tier) {
        return new CommissionBonusTierResponse(
                tier.getId(),
                tier.getMinAmount(),
                tier.getMaxAmount(),
                tier.getBonusPercent(),
                tier.isActive(),
                tier.getCreatedAt(),
                tier.getUpdatedAt());
    }
}
