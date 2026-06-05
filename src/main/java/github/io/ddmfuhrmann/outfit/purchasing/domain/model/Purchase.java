package github.io.ddmfuhrmann.outfit.purchasing.domain.model;

import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseConfirmed;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseLineSnapshot;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchasePayableSnapshot;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "purchase")
public class Purchase extends BaseAggregate<Purchase> {

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status;

    @OneToMany(mappedBy = "purchaseId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchasePayable> payables = new ArrayList<>();

    @Version
    private Long version;

    protected Purchase() {}

    public static Purchase create(Long brandId, Long supplierId, LocalDate purchaseDate, String observations) {
        if (brandId == null) throw new IllegalArgumentException("brandId is required");
        if (purchaseDate == null) throw new IllegalArgumentException("purchaseDate is required");

        var purchase = new Purchase();
        purchase.brandId = brandId;
        purchase.supplierId = supplierId;
        purchase.purchaseDate = purchaseDate;
        purchase.observations = observations;
        purchase.status = PurchaseStatus.OPEN;
        return purchase;
    }

    public void addLine(Long productSkuId, int quantity, BigDecimal unitCost) {
        if (status != PurchaseStatus.OPEN)
            throw new IllegalStateException("Cannot add lines to a purchase that is not OPEN");
        lines.add(PurchaseLine.create(getId(), productSkuId, quantity, unitCost));
    }

    public void addPayable(LocalDate dueDate, BigDecimal amount) {
        if (status != PurchaseStatus.OPEN)
            throw new IllegalStateException("Cannot add payables to a purchase that is not OPEN");
        payables.add(PurchasePayable.create(getId(), dueDate, amount));
    }

    public void removePayable(Long payableId) {
        if (status != PurchaseStatus.OPEN)
            throw new IllegalStateException("Cannot remove payables from a purchase that is not OPEN");
        var payable = payables.stream()
                .filter(p -> p.getId().equals(payableId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payable not found: " + payableId));
        payables.remove(payable);
    }

    public void confirm() {
        if (status != PurchaseStatus.OPEN)
            throw new IllegalStateException("Cannot confirm a purchase that is not OPEN");
        if (lines.isEmpty())
            throw new IllegalStateException("Cannot confirm a purchase with no lines");
        if (payables.isEmpty())
            throw new IllegalStateException("Cannot confirm a purchase with no payables");

        BigDecimal linesTotal = totalLinesAmount();
        BigDecimal payablesTotal = payables.stream()
                .map(PurchasePayable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (payablesTotal.subtract(linesTotal).abs().compareTo(new BigDecimal("0.01")) > 0)
            throw new IllegalStateException("Payables total must match lines total within 0.01");

        status = PurchaseStatus.CONFIRMED;
        registerEvent(buildConfirmedEvent());
    }

    public void cancel() {
        if (status != PurchaseStatus.OPEN)
            throw new IllegalStateException("Cannot cancel a purchase that is not OPEN");
        status = PurchaseStatus.CANCELLED;
    }

    private BigDecimal totalLinesAmount() {
        return lines.stream()
                .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PurchaseConfirmed buildConfirmedEvent() {
        var lineSnapshots = lines.stream()
                .map(l -> new PurchaseLineSnapshot(l.getProductSkuId(), l.getQuantity(), l.getUnitCost()))
                .toList();
        var payableSnapshots = payables.stream()
                .map(p -> new PurchasePayableSnapshot(p.getDueDate(), p.getAmount()))
                .toList();
        return new PurchaseConfirmed(getId(), brandId, supplierId, purchaseDate, observations,
                lineSnapshots, payableSnapshots);
    }
}
