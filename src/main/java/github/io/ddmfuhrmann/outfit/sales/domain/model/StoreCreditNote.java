package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.sales.domain.event.StoreCreditItemSnapshot;
import github.io.ddmfuhrmann.outfit.sales.domain.event.StoreCreditNoteConsumed;
import github.io.ddmfuhrmann.outfit.sales.domain.event.StoreCreditNoteCreated;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "store_credit_note")
public class StoreCreditNote extends BaseAggregate<StoreCreditNote> {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreCreditNoteStatus status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "consumed_by_sale_id")
    private Long consumedBySaleId;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "storeCreditNoteId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreCreditItem> items = new ArrayList<>();

    @Version
    private Long version;

    protected StoreCreditNote() {}

    public static StoreCreditNote create(Long customerId, String notes,
                                         List<StoreCreditItemInput> itemInputs) {
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        if (itemInputs == null || itemInputs.isEmpty())
            throw new IllegalArgumentException("items must not be empty");

        var note = new StoreCreditNote();
        note.customerId = customerId;
        note.notes = notes;
        note.status = StoreCreditNoteStatus.OPEN;

        for (var input : itemInputs) {
            note.items.add(StoreCreditItem.create(note.getId(), input.skuId(), input.productId(),
                    input.quantity(), input.unitPrice()));
        }

        note.totalAmount = note.items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var itemSnapshots = note.items.stream()
                .map(i -> new StoreCreditItemSnapshot(i.getSkuId(), i.getProductId(),
                        i.getQuantity(), i.getUnitPrice()))
                .toList();

        note.registerEvent(new StoreCreditNoteCreated(note.getId(), customerId, note.totalAmount, itemSnapshots));
        return note;
    }

    public void consume(Long saleId) {
        if (status == StoreCreditNoteStatus.CONSUMED)
            throw new IllegalStateException("store credit note is already consumed");
        this.status = StoreCreditNoteStatus.CONSUMED;
        this.consumedBySaleId = saleId;
        registerEvent(new StoreCreditNoteConsumed(getId(), customerId, saleId));
    }

    public record StoreCreditItemInput(Long skuId, Long productId, int quantity, BigDecimal unitPrice) {}
}
