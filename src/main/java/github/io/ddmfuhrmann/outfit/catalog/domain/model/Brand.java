package github.io.ddmfuhrmann.outfit.catalog.domain.model;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.BrandRenamed;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "brand")
public class Brand extends BaseAggregate<Brand> {

    @Column(nullable = false, length = 100)
    private String description;

    protected Brand() {}

    public static Brand create(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        var brand = new Brand();
        brand.description = description.trim();
        return brand;
    }

    public void rename(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        this.description = description.trim();
        registerEvent(new BrandRenamed(getId(), this.description));
    }
}
