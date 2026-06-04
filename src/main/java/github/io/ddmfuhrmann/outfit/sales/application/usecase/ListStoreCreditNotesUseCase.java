package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.StoreCreditNoteResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditNote;
import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditNoteStatus;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.StoreCreditNoteRepository;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListStoreCreditNotesUseCase {

    private final StoreCreditNoteRepository repository;

    public ListStoreCreditNotesUseCase(StoreCreditNoteRepository repository) {
        this.repository = repository;
    }

    public record ListStoreCreditNotesQuery(
            Long customerId,
            StoreCreditNoteStatus status,
            int page,
            int size) {}

    public PageResponse<StoreCreditNoteResponse> execute(ListStoreCreditNotesQuery query) {
        var spec = buildSpec(query);
        var pageable = PageRequest.of(query.page(), query.size());
        var page = repository.findAll(spec, pageable);
        return PageResponse.from(page.map(StoreCreditNoteResponse::from));
    }

    private Specification<StoreCreditNote> buildSpec(ListStoreCreditNotesQuery query) {
        Specification<StoreCreditNote> spec = Specification.where(null);

        if (query.customerId() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("customerId"), query.customerId()));
        }
        if (query.status() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), query.status()));
        }
        return spec;
    }
}
