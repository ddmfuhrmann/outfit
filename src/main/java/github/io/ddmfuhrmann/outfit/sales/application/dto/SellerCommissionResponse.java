package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.SellerCommission;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SellerCommissionResponse(
        Long id,
        Long saleId,
        Long salespersonId,
        LocalDate saleDate,
        BigDecimal commissionPercent,
        BigDecimal immediateAmount,
        BigDecimal deferredAmount,
        BigDecimal commissionBase,
        BigDecimal earnedAmount,
        BigDecimal bonusAmount,
        BigDecimal pendingAmount,
        BigDecimal netAmount,
        String status,
        Instant createdAt) {

    public static SellerCommissionResponse from(SellerCommission c) {
        return new SellerCommissionResponse(
                c.getId(),
                c.getSaleId(),
                c.getSalespersonId(),
                c.getSaleDate(),
                c.getCommissionPercent(),
                c.getImmediateAmount(),
                c.getDeferredAmount(),
                c.getCommissionBase(),
                c.getEarnedAmount(),
                c.getBonusAmount(),
                c.getPendingAmount(),
                c.getNetAmount(),
                c.getStatus().name(),
                c.getCreatedAt());
    }
}
