package github.io.ddmfuhrmann.outfit.sales.domain.repository;

import github.io.ddmfuhrmann.outfit.sales.domain.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {}
