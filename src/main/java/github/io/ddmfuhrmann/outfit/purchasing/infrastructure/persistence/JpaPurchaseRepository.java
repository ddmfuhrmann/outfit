package github.io.ddmfuhrmann.outfit.purchasing.infrastructure.persistence;

import github.io.ddmfuhrmann.outfit.purchasing.domain.model.Purchase;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.purchasing.domain.repository.PurchaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface JpaPurchaseRepository extends JpaRepository<Purchase, Long>, PurchaseRepository {

    Optional<Purchase> findByBrandIdAndPurchaseDateAndStatus(Long brandId, LocalDate purchaseDate, PurchaseStatus status);

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.lines WHERE p.brandId = :brandId AND p.purchaseDate = :purchaseDate AND p.status = :status")
    Optional<Purchase> findWithLinesByBrandIdAndPurchaseDateAndStatus(
            @Param("brandId") Long brandId,
            @Param("purchaseDate") LocalDate purchaseDate,
            @Param("status") PurchaseStatus status);

    @Query("SELECT DISTINCT p FROM Purchase p LEFT JOIN FETCH p.lines WHERE p.brandId = :brandId")
    java.util.List<Purchase> findAllWithLinesByBrandId(@Param("brandId") Long brandId);
}
