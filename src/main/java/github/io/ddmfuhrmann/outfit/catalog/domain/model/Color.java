package github.io.ddmfuhrmann.outfit.catalog.domain.model;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.ColorRenamed;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "color")
public class Color extends BaseAggregate<Color> {

    @Column(nullable = false, length = 100)
    private String description;

    protected Color() {}

    public static Color create(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        var color = new Color();
        color.description = description.trim();
        return color;
    }

    public void rename(String description) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        this.description = description.trim();
        registerEvent(new ColorRenamed(getId(), this.description));
    }
}
