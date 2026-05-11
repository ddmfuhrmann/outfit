package github.io.ddmfuhrmann.outfit.inventory.domain.model;

import github.io.ddmfuhrmann.outfit.inventory.domain.event.StockEntryRecorded;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "stock_entry")
public class StockEntry extends BaseAggregate<StockEntry> {

    private Long productSkuId;
    private Long productId;
    private int quantity;
    private int runningBalance;

    @Enumerated(EnumType.STRING)
    private StockSource source;

    private Long sourceKey;
    private Instant occurredAt;

    protected StockEntry() {}

    public static StockEntry create(Long skuId, Long productId, int quantity, int currentBalance,
                                    StockSource source, Long sourceKey, Instant occurredAt) {
        var entry = new StockEntry();
        entry.productSkuId = skuId;
        entry.productId = productId;
        entry.quantity = quantity;
        entry.runningBalance = currentBalance + quantity;
        entry.source = source;
        entry.sourceKey = sourceKey;
        entry.occurredAt = occurredAt;
        entry.registerEvent(new StockEntryRecorded(
                entry.getId(), skuId, productId, quantity, entry.runningBalance,
                source, sourceKey, occurredAt));
        return entry;
    }
}
