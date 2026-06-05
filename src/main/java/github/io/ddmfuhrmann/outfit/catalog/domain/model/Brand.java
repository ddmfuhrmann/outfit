package github.io.ddmfuhrmann.outfit.catalog.domain.model;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.BrandCreated;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.BrandDeleted;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.BrandRenamed;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "brand")
public class Brand extends BaseAggregate<Brand> {

    @Column(nullable = false, length = 100)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "brand_supplier", joinColumns = @JoinColumn(name = "brand_id"))
    @Column(name = "supplier_id")
    private List<Long> supplierIds = new ArrayList<>();

    protected Brand() {}

    public static Brand create(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        var brand = new Brand();
        brand.description = description.trim();
        brand.registerEvent(new BrandCreated(brand.getId(), brand.description));
        return brand;
    }

    public void rename(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        this.description = description.trim();
        registerEvent(new BrandRenamed(getId(), this.description));
    }

    public void delete() {
        registerEvent(new BrandDeleted(getId()));
    }

    public void addSupplier(Long supplierId) {
        if (supplierId == null) throw new IllegalArgumentException("supplierId is required");
        if (supplierIds.contains(supplierId)) throw new IllegalStateException("supplier already associated with brand");
        supplierIds.add(supplierId);
    }

    public void removeSupplier(Long supplierId) {
        if (supplierId == null) throw new IllegalArgumentException("supplierId is required");
        boolean removed = supplierIds.remove(supplierId);
        if (!removed) throw new IllegalStateException("supplier not associated with brand: " + supplierId);
    }
}
