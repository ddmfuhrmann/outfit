package github.io.ddmfuhrmann.outfit.sales.domain.repository;

import github.io.ddmfuhrmann.outfit.sales.domain.model.Consignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConsignmentRepository extends JpaRepository<Consignment, Long>, JpaSpecificationExecutor<Consignment> {}
