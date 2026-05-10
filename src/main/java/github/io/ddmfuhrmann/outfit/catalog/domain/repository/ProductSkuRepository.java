package github.io.ddmfuhrmann.outfit.catalog.domain.repository;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    Optional<ProductSku> findByBarcode(String barcode);
}
