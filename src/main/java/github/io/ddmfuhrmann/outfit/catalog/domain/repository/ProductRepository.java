package github.io.ddmfuhrmann.outfit.catalog.domain.repository;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByColorId(Long colorId);
    boolean existsByBrandId(Long brandId);
    boolean existsByCategoryId(Long categoryId);
}
