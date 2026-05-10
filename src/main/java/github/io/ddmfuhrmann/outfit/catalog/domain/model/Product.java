package github.io.ddmfuhrmann.outfit.catalog.domain.model;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.*;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "product")
public class Product extends BaseAggregate<Product> {

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cost;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "color_id")
    private Long colorId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSku> skus = new ArrayList<>();

    protected Product() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String description;
        private BigDecimal price;
        private BigDecimal cost;
        private LocalDate purchaseDate;
        private Long colorId;
        private Long brandId;
        private Long categoryId;

        private Builder() {}

        public Builder description(String description)    { this.description = description;       return this; }
        public Builder price(BigDecimal price)            { this.price = price;                   return this; }
        public Builder cost(BigDecimal cost)              { this.cost = cost;                     return this; }
        public Builder purchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate;   return this; }
        public Builder colorId(Long colorId)              { this.colorId = colorId;               return this; }
        public Builder brandId(Long brandId)              { this.brandId = brandId;               return this; }
        public Builder categoryId(Long categoryId)        { this.categoryId = categoryId;         return this; }

        public Product build() {
            validate(description, price, cost, brandId, categoryId);

            var p = new Product();
            p.description = description.trim();
            p.price = price;
            p.cost = cost;
            p.purchaseDate = purchaseDate;
            p.colorId = colorId;
            p.brandId = brandId;
            p.categoryId = categoryId;
            p.active = true;
            p.registerEvent(new ProductCreated(p.getId(), p.toSnapshot()));
            return p;
        }
    }

    public ProductSku addSku(String barcode, Long sizeId, int implantationQty) {
        if (barcode == null || barcode.isBlank()) throw new IllegalArgumentException("barcode is required");
        boolean duplicate = skus.stream().anyMatch(s -> s.getBarcode().equals(barcode.trim()));
        if (duplicate) throw new IllegalArgumentException("barcode already exists in this product");
        var sku = ProductSku.create(getId(), barcode, sizeId);
        skus.add(sku);
        registerEvent(new ProductSkuCreated(sku.getId(), getId(), sizeId, sku.getBarcode(), implantationQty, toSnapshot()));
        return sku;
    }

    public void deactivateSku(Long skuId) {
        var sku = skus.stream()
                .filter(s -> skuId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("sku not found: " + skuId));
        sku.deactivate();
        registerEvent(new ProductSkuDeactivated(skuId, getId(), toSnapshot()));
    }

    public void updateDetails(String description, BigDecimal price, BigDecimal cost,
                              LocalDate purchaseDate, Long colorId, Long brandId, Long categoryId) {
        validate(description, price, cost, brandId, categoryId);

        this.description = description.trim();
        this.price = price;
        this.cost = cost;
        this.purchaseDate = purchaseDate;
        this.colorId = colorId;
        this.brandId = brandId;
        this.categoryId = categoryId;
        registerEvent(new ProductUpdated(getId(), toSnapshot()));
    }

    public void deactivate() {
        if (!active) throw new IllegalStateException("product is already inactive");
        this.active = false;
        registerEvent(new ProductDeactivated(getId(), toSnapshot()));
    }

    private static void validate(String description, BigDecimal price, BigDecimal cost,
                                 Long brandId, Long categoryId) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        if (price == null) throw new IllegalArgumentException("price is required");
        if (price.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("price must be non-negative");
        if (cost == null) throw new IllegalArgumentException("cost is required");
        if (cost.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("cost must be non-negative");
        if (brandId == null) throw new IllegalArgumentException("brandId is required");
        if (categoryId == null) throw new IllegalArgumentException("categoryId is required");
    }

    private ProductSnapshot toSnapshot() {
        return new ProductSnapshot(
                getId(), description, price, cost, purchaseDate,
                colorId, brandId, categoryId, active,
                getCreatedAt(), getUpdatedAt(),
                skus.stream()
                        .map(s -> new ProductSkuSnapshot(s.getId(), s.getBarcode(), s.getSizeId(), s.isActive()))
                        .toList());
    }
}
