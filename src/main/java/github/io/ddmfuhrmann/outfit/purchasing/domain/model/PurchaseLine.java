package github.io.ddmfuhrmann.outfit.purchasing.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "purchase_line")
public class PurchaseLine extends BaseEntity {

    @Column(name = "purchase_id", nullable = false)
    private Long purchaseId;

    @Column(name = "product_sku_id", nullable = false)
    private Long productSkuId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost;

    protected PurchaseLine() {}

    static PurchaseLine create(Long purchaseId, Long productSkuId, int quantity, BigDecimal unitCost) {
        if (productSkuId == null) throw new IllegalArgumentException("productSkuId is required");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be at least 1");
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("unitCost must be non-negative");

        var line = new PurchaseLine();
        line.purchaseId = purchaseId;
        line.productSkuId = productSkuId;
        line.quantity = quantity;
        line.unitCost = unitCost;
        return line;
    }
}
