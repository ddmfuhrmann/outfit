package github.io.ddmfuhrmann.outfit.inventory.domain.repository;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

    Page<StockEntry> findByProductSkuIdOrderByOccurredAtDesc(Long skuId, Pageable pageable);

    @Query("SELECT e.productId FROM StockEntry e WHERE e.productSkuId = :skuId ORDER BY e.occurredAt DESC LIMIT 1")
    Optional<Long> findProductIdByProductSkuId(@Param("skuId") Long skuId);

    @Query("SELECT e.productSkuId, e.productId FROM StockEntry e WHERE e.productSkuId IN :skuIds GROUP BY e.productSkuId, e.productId")
    List<Object[]> findProductIdsBySkuIds(@Param("skuIds") List<Long> skuIds);
}
