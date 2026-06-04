package github.io.ddmfuhrmann.outfit.inventory.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "stock_recount")
public class StockRecount extends BaseAggregate<StockRecount> {

    private LocalDate date;
    private String notes;

    @Enumerated(EnumType.STRING)
    private StockRecountStatus status;

    @SuppressWarnings("java:S1450") // persistent JPA field — read via @Getter, not directly in class body
    private Instant closedAt;

    @OneToMany(mappedBy = "stockRecountId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockRecountItem> items = new ArrayList<>();

    protected StockRecount() {}

    public static StockRecount create(LocalDate date, String notes) {
        var recount = new StockRecount();
        recount.date = date;
        recount.notes = notes;
        recount.status = StockRecountStatus.OPEN;
        return recount;
    }

    public StockRecountItem addItem(Long skuId, int countedQty) {
        if (status == StockRecountStatus.CLOSED) {
            throw new IllegalStateException("Recount is already closed");
        }
        boolean duplicate = items.stream().anyMatch(i -> i.getProductSkuId().equals(skuId));
        if (duplicate) {
            throw new IllegalArgumentException("SKU already added to this recount");
        }
        var item = StockRecountItem.create(getId(), skuId, countedQty);
        items.add(item);
        return item;
    }

    public List<StockRecountItem> close(Instant now) {
        if (status == StockRecountStatus.CLOSED) {
            throw new IllegalStateException("Recount is already closed");
        }
        status = StockRecountStatus.CLOSED;
        closedAt = now;
        return Collections.unmodifiableList(items);
    }
}
