package github.io.ddmfuhrmann.outfit.finance.domain.model;

import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivableCreated;
import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivablePaid;
import github.io.ddmfuhrmann.outfit.finance.domain.event.ReceivablePaymentRecorded;
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
@Table(name = "receivable")
public class Receivable extends BaseAggregate<Receivable> {

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "sale_total_deferred_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal saleTotalDeferredAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceivableStatus status;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "receivableId")
    private List<ReceivablePayment> payments = new ArrayList<>();

    protected Receivable() {}

    public static Receivable create(Long saleId, Long customerId, LocalDate dueDate,
                                     BigDecimal amount, BigDecimal saleTotalDeferredAmount) {
        if (saleId == null) throw new IllegalArgumentException("saleId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        if (dueDate == null) throw new IllegalArgumentException("dueDate is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be positive");
        if (saleTotalDeferredAmount == null)
            throw new IllegalArgumentException("saleTotalDeferredAmount is required");

        var receivable = new Receivable();
        receivable.saleId = saleId;
        receivable.customerId = customerId;
        receivable.dueDate = dueDate;
        receivable.amount = amount;
        receivable.balance = amount;
        receivable.saleTotalDeferredAmount = saleTotalDeferredAmount;
        receivable.status = ReceivableStatus.OPEN;

        receivable.registerEvent(new ReceivableCreated(
                receivable.getId(), saleId, customerId, dueDate, amount));
        return receivable;
    }

    public void recordPayment(BigDecimal paymentAmount, Instant now) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("payment amount must be positive");
        if (status == ReceivableStatus.PAID)
            throw new IllegalStateException("receivable is already PAID");
        if (paymentAmount.compareTo(balance) > 0)
            throw new IllegalArgumentException("payment amount exceeds balance");

        payments.add(ReceivablePayment.create(getId(), now, paymentAmount));
        balance = balance.subtract(paymentAmount);

        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            status = ReceivableStatus.PAID;
            registerEvent(new ReceivablePaymentRecorded(getId(), balance, status.name()));
            registerEvent(new ReceivablePaid(getId(), saleId, amount, saleTotalDeferredAmount));
        } else {
            status = ReceivableStatus.PARTIALLY_PAID;
            registerEvent(new ReceivablePaymentRecorded(getId(), balance, status.name()));
        }
    }
}
