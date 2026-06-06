package github.io.ddmfuhrmann.outfit.finance.domain.model;

import github.io.ddmfuhrmann.outfit.finance.domain.event.PayableCancelled;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayableCreated;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayablePaid;
import github.io.ddmfuhrmann.outfit.finance.domain.event.PayablePaymentRecorded;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "payable")
public class Payable extends BaseAggregate<Payable> {

    @Column(name = "purchase_id", nullable = false)
    private Long purchaseId;

    @Column(name = "purchase_payable_id", nullable = false)
    private Long purchasePayableId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayableStatus status;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "payableId")
    private List<PayablePayment> payments = new ArrayList<>();

    protected Payable() {}

    public static Payable create(Long purchaseId, Long purchasePayableId, Long supplierId,
                                  LocalDate dueDate, BigDecimal amount) {
        if (purchaseId == null) throw new IllegalArgumentException("purchaseId is required");
        if (purchasePayableId == null) throw new IllegalArgumentException("purchasePayableId is required");
        if (supplierId == null) throw new IllegalArgumentException("supplierId is required");
        if (dueDate == null) throw new IllegalArgumentException("dueDate is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be positive");

        var payable = new Payable();
        payable.purchaseId = purchaseId;
        payable.purchasePayableId = purchasePayableId;
        payable.supplierId = supplierId;
        payable.dueDate = dueDate;
        payable.amount = amount;
        payable.balance = amount;
        payable.status = PayableStatus.OPEN;

        payable.registerEvent(new PayableCreated(
                payable.getId(), purchaseId, supplierId, dueDate, amount));
        return payable;
    }

    public void recordPayment(BigDecimal paymentAmount, Instant now) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("payment amount must be positive");
        if (status == PayableStatus.PAID)
            throw new IllegalStateException("payable is already PAID");
        if (status == PayableStatus.CANCELLED)
            throw new IllegalStateException("payable is CANCELLED");
        if (paymentAmount.compareTo(balance) > 0)
            throw new IllegalArgumentException("payment amount exceeds balance");

        payments.add(PayablePayment.create(getId(), now, paymentAmount));
        balance = balance.subtract(paymentAmount);

        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            status = PayableStatus.PAID;
            registerEvent(new PayablePaymentRecorded(getId(), balance, status.name()));
            registerEvent(new PayablePaid(getId(), purchaseId));
        } else {
            status = PayableStatus.PARTIALLY_PAID;
            registerEvent(new PayablePaymentRecorded(getId(), balance, status.name()));
        }
    }

    public void cancel() {
        if (status == PayableStatus.CANCELLED)
            throw new IllegalStateException("payable is already cancelled");
        if (status == PayableStatus.PAID)
            throw new IllegalStateException("cannot cancel a PAID payable");

        status = PayableStatus.CANCELLED;
        registerEvent(new PayableCancelled(getId(), purchaseId));
    }
}
