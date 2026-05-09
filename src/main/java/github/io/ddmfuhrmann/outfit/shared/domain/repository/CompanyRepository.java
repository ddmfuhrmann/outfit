package github.io.ddmfuhrmann.outfit.shared.domain.repository;

import github.io.ddmfuhrmann.outfit.shared.domain.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
