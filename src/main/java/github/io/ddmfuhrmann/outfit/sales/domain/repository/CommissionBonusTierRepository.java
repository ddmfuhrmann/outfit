package github.io.ddmfuhrmann.outfit.sales.domain.repository;

import github.io.ddmfuhrmann.outfit.sales.domain.model.CommissionBonusTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CommissionBonusTierRepository extends JpaRepository<CommissionBonusTier, Long> {

    List<CommissionBonusTier> findByActiveTrue();

    @Query("SELECT t FROM CommissionBonusTier t WHERE t.active = true AND :amount >= t.minAmount AND :amount <= t.maxAmount")
    Optional<CommissionBonusTier> findActiveMatchingTier(@Param("amount") BigDecimal amount);

    @Query("SELECT COUNT(t) > 0 FROM CommissionBonusTier t WHERE t.active = true " +
           "AND (:excludeId IS NULL OR t.id <> :excludeId) " +
           "AND t.minAmount < :max AND t.maxAmount > :min")
    boolean existsOverlappingActiveTier(@Param("min") BigDecimal min,
                                        @Param("max") BigDecimal max,
                                        @Param("excludeId") Long excludeId);
}
