package github.io.ddmfuhrmann.outfit.finance.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "receivable_payment")
public class ReceivablePayment extends BaseEntity {

    @Column(name = "receivable_id", nullable = false)
    private Long receivableId;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    protected ReceivablePayment() {}

    static ReceivablePayment create(Long receivableId, Instant paidAt, BigDecimal amount) {
        var payment = new ReceivablePayment();
        payment.receivableId = receivableId;
        payment.paidAt = paidAt;
        payment.amount = amount;
        return payment;
    }
}
