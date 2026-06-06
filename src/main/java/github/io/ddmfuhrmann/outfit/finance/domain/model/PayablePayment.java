package github.io.ddmfuhrmann.outfit.finance.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "payable_payment")
public class PayablePayment extends BaseEntity {

    @Column(name = "payable_id", nullable = false)
    private Long payableId;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    protected PayablePayment() {}

    static PayablePayment create(Long payableId, Instant paidAt, BigDecimal amount) {
        var payment = new PayablePayment();
        payment.payableId = payableId;
        payment.paidAt = paidAt;
        payment.amount = amount;
        return payment;
    }
}
