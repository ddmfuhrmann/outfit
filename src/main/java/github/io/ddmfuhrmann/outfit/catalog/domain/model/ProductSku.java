package github.io.ddmfuhrmann.outfit.catalog.domain.model;

import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "product_sku")
public class ProductSku extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String barcode;

    @Column(name = "size_id", nullable = false)
    private Long sizeId;

    @Column(nullable = false)
    private boolean active;

    protected ProductSku() {}

    static ProductSku create(Long productId, String barcode, Long sizeId) {
        if (barcode == null || barcode.isBlank()) throw new IllegalArgumentException("barcode is required");
        if (sizeId == null) throw new IllegalArgumentException("sizeId is required");
        var sku = new ProductSku();
        sku.productId = productId;
        sku.barcode = barcode.trim();
        sku.sizeId = sizeId;
        sku.active = true;
        return sku;
    }

    void deactivate() {
        if (!active) throw new IllegalStateException("sku is already inactive");
        this.active = false;
    }
}
