package github.io.ddmfuhrmann.outfit.catalog.domain.model;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.CategoryCreated;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.CategoryDeleted;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.CategoryRenamed;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "category")
public class Category extends BaseAggregate<Category> {

    @Column(nullable = false, length = 100)
    private String description;

    @Column(length = 10)
    private String ncmCode;

    protected Category() {}

    public static Category create(String description, String ncmCode) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        var category = new Category();
        category.description = description.trim();
        category.ncmCode = ncmCode != null ? ncmCode.trim() : null;
        category.registerEvent(new CategoryCreated(category.getId(), category.description));
        return category;
    }

    public void rename(String description, String ncmCode) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        this.description = description.trim();
        this.ncmCode = ncmCode != null ? ncmCode.trim() : null;
        registerEvent(new CategoryRenamed(getId(), this.description));
    }

    public void delete() {
        registerEvent(new CategoryDeleted(getId()));
    }
}
