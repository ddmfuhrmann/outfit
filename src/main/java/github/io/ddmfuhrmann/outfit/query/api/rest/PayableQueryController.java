package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.PayableDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetPayableQueryUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchPayablesUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchPayablesUseCase.SearchPayablesQuery;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/payables")
public class PayableQueryController {

    private final GetPayableQueryUseCase getPayable;
    private final SearchPayablesUseCase searchPayables;

    public PayableQueryController(GetPayableQueryUseCase getPayable,
                                   SearchPayablesUseCase searchPayables) {
        this.getPayable = getPayable;
        this.searchPayables = searchPayables;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payable by ID",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<PayableDocument> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getPayable.execute(id));
    }

    @GetMapping
    @Operation(summary = "Search payables",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<PageResponse<PayableDocument>> search(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new SearchPayablesQuery(supplierId, status, dueDateFrom, dueDateTo, page, size);
        return ResponseEntity.ok(searchPayables.execute(query));
    }
}
