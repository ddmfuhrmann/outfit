package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "consignment_item")
public class ConsignmentItem extends BaseEntity {

    @Column(name = "consignment_id", nullable = false)
    private Long consignmentId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity_issued", nullable = false)
    private int quantityIssued;

    @Column(name = "quantity_returned", nullable = false)
    private int quantityReturned;

    protected ConsignmentItem() {}

    static ConsignmentItem create(Long consignmentId, Long skuId, Long productId, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (unitPrice == null) throw new IllegalArgumentException("unitPrice is required");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("unitPrice must not be negative");

        var item = new ConsignmentItem();
        item.consignmentId = consignmentId;
        item.skuId = skuId;
        item.productId = productId;
        item.quantityIssued = quantity;
        item.unitPrice = unitPrice;
        item.quantityReturned = 0;
        return item;
    }

    public void recordReturn(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (quantityReturned + quantity > quantityIssued)
            throw new IllegalStateException("return exceeds issued quantity");
        this.quantityReturned += quantity;
    }

    public int getQuantitySold() {
        return quantityIssued - quantityReturned;
    }
}
