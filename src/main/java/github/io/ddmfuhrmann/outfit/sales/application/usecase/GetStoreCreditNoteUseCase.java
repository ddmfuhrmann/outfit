package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.StoreCreditNoteResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.StoreCreditNoteRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetStoreCreditNoteUseCase {

    private final StoreCreditNoteRepository repository;

    public GetStoreCreditNoteUseCase(StoreCreditNoteRepository repository) {
        this.repository = repository;
    }

    public StoreCreditNoteResponse execute(Long id) {
        var note = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StoreCreditNote not found: " + id));
        return StoreCreditNoteResponse.from(note);
    }
}
