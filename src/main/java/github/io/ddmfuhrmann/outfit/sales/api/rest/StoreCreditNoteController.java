package github.io.ddmfuhrmann.outfit.sales.api.rest;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateStoreCreditNoteRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.StoreCreditNoteResponse;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.CreateStoreCreditNoteUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.GetStoreCreditNoteUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.ListStoreCreditNotesUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.ListStoreCreditNotesUseCase.ListStoreCreditNotesQuery;
import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditNoteStatus;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/store-credit-notes")
public class StoreCreditNoteController {

    private final CreateStoreCreditNoteUseCase createNote;
    private final GetStoreCreditNoteUseCase getNote;
    private final ListStoreCreditNotesUseCase listNotes;

    public StoreCreditNoteController(CreateStoreCreditNoteUseCase createNote,
                                     GetStoreCreditNoteUseCase getNote,
                                     ListStoreCreditNotesUseCase listNotes) {
        this.createNote = createNote;
        this.getNote = getNote;
        this.listNotes = listNotes;
    }

    @PostMapping
    @Operation(summary = "Create a store credit note")
    ResponseEntity<StoreCreditNoteResponse> create(@RequestBody @Valid CreateStoreCreditNoteRequest request) {
        var response = createNote.execute(request);
        return ResponseEntity.created(URI.create("/store-credit-notes/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store credit note by ID")
    ResponseEntity<StoreCreditNoteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getNote.execute(id));
    }

    @GetMapping
    @Operation(summary = "List store credit notes")
    ResponseEntity<PageResponse<StoreCreditNoteResponse>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) StoreCreditNoteStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListStoreCreditNotesQuery(customerId, status, page, size);
        return ResponseEntity.ok(listNotes.execute(query));
    }
}
