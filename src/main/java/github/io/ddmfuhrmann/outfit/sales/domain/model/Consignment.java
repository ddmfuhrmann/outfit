package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentClosed;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentIssued;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemSnapshot;
import github.io.ddmfuhrmann.outfit.sales.domain.event.ConsignmentItemsReturned;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(name = "consignment")
public class Consignment extends BaseAggregate<Consignment> {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ElementCollection
    @CollectionTable(name = "consignment_salesperson", joinColumns = @JoinColumn(name = "consignment_id"))
    @Column(name = "salesperson_id")
    private List<Long> salespersonIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsignmentStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "consignmentId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsignmentItem> items = new ArrayList<>();

    @Version
    private long version;

    protected Consignment() {}

    public static Consignment create(Long customerId, List<Long> salespersonIds, LocalDate issueDate,
                                     String notes, List<ConsignmentItemInput> itemInputs) {
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
        if (salespersonIds == null || salespersonIds.isEmpty())
            throw new IllegalArgumentException("salespersonIds must not be empty");
        if (issueDate == null) throw new IllegalArgumentException("issueDate is required");
        if (itemInputs == null || itemInputs.isEmpty())
            throw new IllegalArgumentException("itemInputs must not be empty");

        var consignment = new Consignment();
        consignment.customerId = customerId;
        consignment.salespersonIds = new ArrayList<>(salespersonIds);
        consignment.issueDate = issueDate;
        consignment.notes = notes;
        consignment.status = ConsignmentStatus.OPEN;

        for (var input : itemInputs) {
            consignment.items.add(
                    ConsignmentItem.create(consignment.getId(), input.skuId(), input.productId(),
                            input.quantity(), input.unitPrice()));
        }

        consignment.registerEvent(new ConsignmentIssued(
                consignment.getId(),
                customerId,
                List.copyOf(salespersonIds),
                consignment.items.stream()
                        .map(i -> new ConsignmentItemSnapshot(i.getSkuId(), i.getProductId(),
                                i.getQuantityIssued(), i.getUnitPrice()))
                        .toList()));

        return consignment;
    }

    public void returnItems(Map<Long, Integer> quantitiesBySkuId) {
        if (status != ConsignmentStatus.OPEN)
            throw new IllegalStateException("consignment is already closed");

        var returnedSnapshots = new ArrayList<ConsignmentItemSnapshot>();
        for (var entry : quantitiesBySkuId.entrySet()) {
            Long skuId = entry.getKey();
            int qty = entry.getValue();
            var item = items.stream()
                    .filter(i -> skuId.equals(i.getSkuId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("item not found for skuId: " + skuId));
            item.recordReturn(qty);
            returnedSnapshots.add(new ConsignmentItemSnapshot(item.getSkuId(), item.getProductId(), qty, item.getUnitPrice()));
        }

        registerEvent(new ConsignmentItemsReturned(getId(), List.copyOf(returnedSnapshots)));
    }

    public void close() {
        if (status != ConsignmentStatus.OPEN)
            throw new IllegalStateException("consignment is already closed");

        status = ConsignmentStatus.CLOSED;
        closedAt = Instant.now();

        var soldItems = items.stream()
                .filter(i -> i.getQuantitySold() > 0)
                .map(i -> new ConsignmentItemSnapshot(i.getSkuId(), i.getProductId(),
                        i.getQuantitySold(), i.getUnitPrice()))
                .toList();

        registerEvent(new ConsignmentClosed(getId(), customerId, List.copyOf(salespersonIds),
                issueDate, closedAt, soldItems));
    }

    public record ConsignmentItemInput(Long skuId, Long productId, int quantity, BigDecimal unitPrice) {}
}
