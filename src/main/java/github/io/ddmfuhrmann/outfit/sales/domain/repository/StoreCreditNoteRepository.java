package github.io.ddmfuhrmann.outfit.sales.domain.repository;

import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StoreCreditNoteRepository extends JpaRepository<StoreCreditNote, Long>, JpaSpecificationExecutor<StoreCreditNote> {}
