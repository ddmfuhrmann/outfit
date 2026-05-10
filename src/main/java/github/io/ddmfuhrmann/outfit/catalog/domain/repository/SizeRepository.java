package github.io.ddmfuhrmann.outfit.catalog.domain.repository;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Long> {
    boolean existsByDescription(String description);
}
