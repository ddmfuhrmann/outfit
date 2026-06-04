package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "commission_bonus_tier")
public class CommissionBonusTier extends BaseAggregate<CommissionBonusTier> {

    @Column(name = "min_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "bonus_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal bonusPercent;

    @Column(nullable = false)
    private boolean active;

    protected CommissionBonusTier() {}

    public static CommissionBonusTier create(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal bonusPercent) {
        validate(minAmount, maxAmount, bonusPercent);
        var tier = new CommissionBonusTier();
        tier.minAmount = minAmount;
        tier.maxAmount = maxAmount;
        tier.bonusPercent = bonusPercent;
        tier.active = true;
        return tier;
    }

    public void update(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal bonusPercent) {
        validate(minAmount, maxAmount, bonusPercent);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.bonusPercent = bonusPercent;
    }

    public void deactivate() {
        if (!active) throw new IllegalStateException("commission bonus tier is already inactive");
        this.active = false;
    }

    public boolean matches(BigDecimal amount) {
        if (amount == null) return false;
        return amount.compareTo(minAmount) >= 0 && amount.compareTo(maxAmount) <= 0;
    }

    private static void validate(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal bonusPercent) {
        if (minAmount == null || minAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("minAmount must be non-negative");
        if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("maxAmount must be positive");
        if (maxAmount.compareTo(minAmount) <= 0)
            throw new IllegalArgumentException("maxAmount must be greater than minAmount");
        if (bonusPercent == null || bonusPercent.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("bonusPercent must be non-negative");
    }
}
