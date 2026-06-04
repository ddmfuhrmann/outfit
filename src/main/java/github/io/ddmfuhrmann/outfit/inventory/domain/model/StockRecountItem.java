package github.io.ddmfuhrmann.outfit.inventory.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "stock_recount_item")
public class StockRecountItem extends BaseEntity {

    private Long stockRecountId;
    private Long productSkuId;
    private int countedQty;
    @SuppressWarnings("java:S1450") // persistent JPA field — written by recordSystemBalance, read via getter
    private Integer systemBalance;

    protected StockRecountItem() {}

    static StockRecountItem create(Long recountId, Long skuId, int countedQty) {
        if (countedQty < 0) {
            throw new IllegalArgumentException("countedQty must not be negative");
        }
        var item = new StockRecountItem();
        item.stockRecountId = recountId;
        item.productSkuId = skuId;
        item.countedQty = countedQty;
        return item;
    }

    public void recordSystemBalance(int balance) {
        this.systemBalance = balance;
    }
}
