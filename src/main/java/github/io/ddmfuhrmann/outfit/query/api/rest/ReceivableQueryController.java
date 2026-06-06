package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.ReceivableDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetReceivableQueryUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchReceivablesUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchReceivablesUseCase.SearchReceivablesQuery;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/receivables")
public class ReceivableQueryController {

    private final GetReceivableQueryUseCase getReceivable;
    private final SearchReceivablesUseCase searchReceivables;

    public ReceivableQueryController(GetReceivableQueryUseCase getReceivable,
                                     SearchReceivablesUseCase searchReceivables) {
        this.getReceivable = getReceivable;
        this.searchReceivables = searchReceivables;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get receivable by ID",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<ReceivableDocument> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getReceivable.execute(id));
    }

    @GetMapping
    @Operation(summary = "Search receivables",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<PageResponse<ReceivableDocument>> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new SearchReceivablesQuery(customerId, status, dueDateFrom, dueDateTo, page, size);
        return ResponseEntity.ok(searchReceivables.execute(query));
    }
}
