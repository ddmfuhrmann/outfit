package github.io.ddmfuhrmann.outfit.purchasing.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "purchase_payable")
public class PurchasePayable extends BaseEntity {

    @Column(name = "purchase_id", nullable = false)
    private Long purchaseId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    protected PurchasePayable() {}

    static PurchasePayable create(Long purchaseId, LocalDate dueDate, BigDecimal amount) {
        if (dueDate == null) throw new IllegalArgumentException("dueDate is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be positive");

        var payable = new PurchasePayable();
        payable.purchaseId = purchaseId;
        payable.dueDate = dueDate;
        payable.amount = amount;
        return payable;
    }
}
