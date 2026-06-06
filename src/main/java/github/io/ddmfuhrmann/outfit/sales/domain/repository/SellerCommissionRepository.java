package github.io.ddmfuhrmann.outfit.sales.domain.repository;

import github.io.ddmfuhrmann.outfit.sales.domain.model.SellerCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SellerCommissionRepository extends JpaRepository<SellerCommission, Long>, JpaSpecificationExecutor<SellerCommission> {

    List<SellerCommission> findBySaleId(Long saleId);
}
