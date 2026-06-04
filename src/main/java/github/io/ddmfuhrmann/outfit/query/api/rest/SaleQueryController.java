package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.SaleDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetSaleFromIndexUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchSalesUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchSalesUseCase.SearchSalesQuery;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/sales")
public class SaleQueryController {

    private final GetSaleFromIndexUseCase getSale;
    private final SearchSalesUseCase searchSales;

    public SaleQueryController(GetSaleFromIndexUseCase getSale,
                               SearchSalesUseCase searchSales) {
        this.getSale = getSale;
        this.searchSales = searchSales;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sale by ID",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<SaleDocument> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getSale.execute(id));
    }

    @GetMapping
    @Operation(summary = "Search sales",
            description = "Served from Elasticsearch. Synchronous replication — reflects the state after domain events are processed.")
    ResponseEntity<PageResponse<SaleDocument>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new SearchSalesQuery(q, customerId, salespersonId, from, to, page, size);
        return ResponseEntity.ok(searchSales.execute(query));
    }
}
