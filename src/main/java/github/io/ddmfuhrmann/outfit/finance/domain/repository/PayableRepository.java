package github.io.ddmfuhrmann.outfit.finance.domain.repository;

import github.io.ddmfuhrmann.outfit.finance.domain.model.Payable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayableRepository extends JpaRepository<Payable, Long> {

    List<Payable> findByPurchaseId(Long purchaseId);
}
