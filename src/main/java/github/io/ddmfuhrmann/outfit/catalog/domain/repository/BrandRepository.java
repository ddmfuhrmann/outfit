package github.io.ddmfuhrmann.outfit.catalog.domain.repository;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    boolean existsByDescription(String description);
}
