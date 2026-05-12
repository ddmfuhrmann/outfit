package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.ConsignmentDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetConsignmentFromIndexUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchConsignmentsUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchConsignmentsUseCase.SearchConsignmentsQuery;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/consignments")
public class ConsignmentQueryController {

    private final GetConsignmentFromIndexUseCase getConsignment;
    private final SearchConsignmentsUseCase searchConsignments;

    public ConsignmentQueryController(GetConsignmentFromIndexUseCase getConsignment,
                                      SearchConsignmentsUseCase searchConsignments) {
        this.getConsignment = getConsignment;
        this.searchConsignments = searchConsignments;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consignment by ID",
            description = "Served from Elasticsearch. Reflects indexed state after domain events are processed (synchronous).")
    ResponseEntity<ConsignmentDocument> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getConsignment.execute(id));
    }

    @GetMapping
    @Operation(summary = "Search consignments",
            description = "Served from Elasticsearch. Reflects indexed state after domain events are processed (synchronous).")
    ResponseEntity<PageResponse<ConsignmentDocument>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new SearchConsignmentsQuery(q, customerId, salespersonId, status, from, to, page, size);
        return ResponseEntity.ok(searchConsignments.execute(query));
    }
}
