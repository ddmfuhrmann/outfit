package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "store_credit_item")
public class StoreCreditItem extends BaseEntity {

    @Column(name = "store_credit_note_id", nullable = false)
    private Long storeCreditNoteId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    protected StoreCreditItem() {}

    static StoreCreditItem create(Long storeCreditNoteId, Long skuId, Long productId, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("unitPrice must be non-negative");

        var item = new StoreCreditItem();
        item.storeCreditNoteId = storeCreditNoteId;
        item.skuId = skuId;
        item.productId = productId;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        return item;
    }
}
