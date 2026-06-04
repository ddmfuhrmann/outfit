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
    private long version;

    protected SellerCommission() {}

    public static SellerCommission create(Long saleId, Long salespersonId, LocalDate saleDate,
                                          BigDecimal commissionPercent, BigDecimal netAmount,
                                          BigDecimal sharePercent,
                                          List<SaleInstallmentSnapshot> installments,
                                          BigDecimal bonusPercent) {
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

        CommissionStatus commissionStatus;
        if (pending.compareTo(BigDecimal.ZERO) == 0) {
            commissionStatus = CommissionStatus.EARNED;
        } else if (earned.compareTo(BigDecimal.ZERO) > 0) {
            commissionStatus = CommissionStatus.PARTIAL;
        } else {
            commissionStatus = CommissionStatus.PENDING;
        }

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
}
