package github.io.ddmfuhrmann.outfit.sales.api.rest;

import github.io.ddmfuhrmann.outfit.sales.application.dto.SellerCommissionResponse;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.GetCommissionUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.ListCommissionsUseCase;
import github.io.ddmfuhrmann.outfit.sales.application.usecase.ListCommissionsUseCase.ListCommissionsQuery;
import github.io.ddmfuhrmann.outfit.sales.domain.model.CommissionStatus;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/commissions")
public class CommissionController {

    private final GetCommissionUseCase getCommission;
    private final ListCommissionsUseCase listCommissions;

    public CommissionController(GetCommissionUseCase getCommission,
                                ListCommissionsUseCase listCommissions) {
        this.getCommission = getCommission;
        this.listCommissions = listCommissions;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get commission by ID")
    ResponseEntity<SellerCommissionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getCommission.execute(id));
    }

    @GetMapping
    @Operation(summary = "List commissions")
    ResponseEntity<PageResponse<SellerCommissionResponse>> list(
            @RequestParam(required = false) Long salespersonId,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListCommissionsQuery(salespersonId, status, from, to, page, size);
        return ResponseEntity.ok(listCommissions.execute(query));
    }
}
