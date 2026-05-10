package github.io.ddmfuhrmann.outfit.catalog.domain.model;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.SizeCreated;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.SizeDeleted;
import github.io.ddmfuhrmann.outfit.catalog.domain.event.SizeRenamed;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "size")
public class Size extends BaseAggregate<Size> {

    @Column(nullable = false, length = 100)
    private String description;

    protected Size() {}

    public static Size create(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        var size = new Size();
        size.description = description.trim();
        size.registerEvent(new SizeCreated(size.getId(), size.description));
        return size;
    }

    public void rename(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        this.description = description.trim();
        registerEvent(new SizeRenamed(getId(), this.description));
    }

    public void delete() {
        registerEvent(new SizeDeleted(getId()));
    }
}
