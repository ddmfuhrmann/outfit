package github.io.ddmfuhrmann.outfit.catalog.domain.repository;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {
    boolean existsByDescription(String description);
}
