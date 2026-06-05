package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleInstallmentSnapshot;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Getter
@Entity
@Table(name = "seller_commission")
public class SellerCommission extends BaseAggregate<SellerCommission> {

    private static final BigDecimal DEFERRED_WEIGHT = new BigDecimal("0.7");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "salesperson_id", nullable = false)
    private Long salespersonId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "commission_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercent;

    @Column(name = "immediate_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal immediateAmount;

    @Column(name = "deferred_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal deferredAmount;

    @Column(name = "commission_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionBase;

    @Column(name = "earned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal earnedAmount;

    @Column(name = "bonus_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "pending_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal pendingAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommissionStatus status;

    @Version
    private Long version;

    protected SellerCommission() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long saleId;
        private Long salespersonId;
        private LocalDate saleDate;
        private BigDecimal commissionPercent;
        private BigDecimal netAmount;
        private BigDecimal sharePercent;
        private List<SaleInstallmentSnapshot> installments;
        private BigDecimal bonusPercent;

        public Builder saleId(Long saleId)                             { this.saleId = saleId; return this; }
        public Builder salespersonId(Long salespersonId)               { this.salespersonId = salespersonId; return this; }
        public Builder saleDate(LocalDate saleDate)                    { this.saleDate = saleDate; return this; }
        public Builder commissionPercent(BigDecimal commissionPercent) { this.commissionPercent = commissionPercent; return this; }
        public Builder netAmount(BigDecimal netAmount)                  { this.netAmount = netAmount; return this; }
        public Builder sharePercent(BigDecimal sharePercent)           { this.sharePercent = sharePercent; return this; }
        public Builder installments(List<SaleInstallmentSnapshot> installments) { this.installments = installments; return this; }
        public Builder bonusPercent(BigDecimal bonusPercent)           { this.bonusPercent = bonusPercent; return this; }

        public SellerCommission build() {
            if (saleId == null) throw new IllegalArgumentException("saleId is required");
            if (salespersonId == null) throw new IllegalArgumentException("salespersonId is required");
            if (commissionPercent == null) throw new IllegalArgumentException("commissionPercent is required");
            if (netAmount == null) throw new IllegalArgumentException("netAmount is required");
            if (sharePercent == null) throw new IllegalArgumentException("sharePercent is required");

            BigDecimal sellerNetAmount = netAmount.multiply(sharePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            BigDecimal immediate = installments.stream()
                    .filter(i -> !i.isDeferred())
                    .map(SaleInstallmentSnapshot::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .multiply(sharePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            BigDecimal deferred = installments.stream()
                    .filter(SaleInstallmentSnapshot::isDeferred)
                    .map(SaleInstallmentSnapshot::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .multiply(sharePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            BigDecimal base = immediate.add(deferred.multiply(DEFERRED_WEIGHT)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal earned = base.multiply(commissionPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            BigDecimal bonus = (bonusPercent != null && bonusPercent.compareTo(BigDecimal.ZERO) > 0)
                    ? sellerNetAmount.multiply(bonusPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal pending = deferred.multiply(commissionPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            CommissionStatus commissionStatus = resolveStatus(pending, earned);

            var commission = new SellerCommission();
            commission.saleId = saleId;
            commission.salespersonId = salespersonId;
            commission.saleDate = saleDate;
            commission.commissionPercent = commissionPercent;
            commission.immediateAmount = immediate;
            commission.deferredAmount = deferred;
            commission.commissionBase = base;
            commission.earnedAmount = earned;
            commission.bonusAmount = bonus;
            commission.pendingAmount = pending;
            commission.netAmount = sellerNetAmount;
            commission.status = commissionStatus;
            return commission;
        }

        private CommissionStatus resolveStatus(BigDecimal pending, BigDecimal earned) {
            if (pending.compareTo(BigDecimal.ZERO) == 0) return CommissionStatus.EARNED;
            if (earned.compareTo(BigDecimal.ZERO) > 0)  return CommissionStatus.PARTIAL;
            return CommissionStatus.PENDING;
        }
    }
}
