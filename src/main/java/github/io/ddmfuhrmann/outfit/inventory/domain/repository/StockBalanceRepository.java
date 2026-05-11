package github.io.ddmfuhrmann.outfit.inventory.domain.repository;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBalance b WHERE b.productSkuId = :skuId")
    Optional<StockBalance> findAndLock(@Param("skuId") Long skuId);
}
