package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.PurchaseDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetPurchaseQueryUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchPurchasesUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchPurchasesUseCase.SearchPurchasesQuery;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/purchases")
public class PurchaseQueryController {

    private final GetPurchaseQueryUseCase getPurchaseUseCase;
    private final SearchPurchasesUseCase searchPurchasesUseCase;

    public PurchaseQueryController(GetPurchaseQueryUseCase getPurchaseUseCase,
                                   SearchPurchasesUseCase searchPurchasesUseCase) {
        this.getPurchaseUseCase = getPurchaseUseCase;
        this.searchPurchasesUseCase = searchPurchasesUseCase;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase by ID",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<PurchaseDocument> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getPurchaseUseCase.execute(id));
    }

    @GetMapping
    @Operation(summary = "Search purchases",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<PageResponse<PurchaseDocument>> search(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new SearchPurchasesQuery(brandId, supplierId, status, from, to, page, size);
        return ResponseEntity.ok(searchPurchasesUseCase.execute(query));
    }
}
