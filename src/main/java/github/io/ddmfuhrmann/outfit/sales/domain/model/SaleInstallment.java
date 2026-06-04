package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "sale_installment")
public class SaleInstallment extends BaseEntity {

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_modality", nullable = false, length = 30)
    private PaymentModality paymentModality;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    protected SaleInstallment() {}

    static SaleInstallment create(Long saleId, PaymentModality paymentModality, LocalDate dueDate, BigDecimal amount) {
        if (paymentModality == null) throw new IllegalArgumentException("paymentModality is required");
        if (dueDate == null) throw new IllegalArgumentException("dueDate is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be positive");

        var installment = new SaleInstallment();
        installment.saleId = saleId;
        installment.paymentModality = paymentModality;
        installment.dueDate = dueDate;
        installment.amount = amount;
        return installment;
    }
}
