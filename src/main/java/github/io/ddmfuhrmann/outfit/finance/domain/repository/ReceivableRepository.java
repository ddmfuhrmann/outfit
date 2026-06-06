package github.io.ddmfuhrmann.outfit.finance.domain.repository;

import github.io.ddmfuhrmann.outfit.finance.domain.model.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {}
