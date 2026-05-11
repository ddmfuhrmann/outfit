package github.io.ddmfuhrmann.outfit.inventory.domain.repository;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

    Page<StockEntry> findByProductSkuIdOrderByOccurredAtDesc(Long skuId, Pageable pageable);
}
