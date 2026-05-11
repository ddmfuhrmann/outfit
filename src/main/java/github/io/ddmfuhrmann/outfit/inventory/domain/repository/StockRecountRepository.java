package github.io.ddmfuhrmann.outfit.inventory.domain.repository;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockRecount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRecountRepository extends JpaRepository<StockRecount, Long> {
}
