package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateStoreCreditNoteRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.StoreCreditNoteResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditNote;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.StoreCreditNoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CreateStoreCreditNoteUseCase {

    private final StoreCreditNoteRepository repository;

    public CreateStoreCreditNoteUseCase(StoreCreditNoteRepository repository) {
        this.repository = repository;
    }

    public StoreCreditNoteResponse execute(CreateStoreCreditNoteRequest request) {
        var itemInputs = request.items().stream()
                .map(i -> new StoreCreditNote.StoreCreditItemInput(
                        i.skuId(), i.productId(), i.quantity(), i.unitPrice()))
                .toList();

        var note = StoreCreditNote.create(request.customerId(), request.notes(), itemInputs);
        repository.save(note);

        log.info("StoreCreditNote {} created for customer {}", note.getId(), request.customerId());
        return StoreCreditNoteResponse.from(note);
    }
}
