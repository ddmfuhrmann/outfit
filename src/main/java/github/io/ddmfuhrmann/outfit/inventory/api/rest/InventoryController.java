package github.io.ddmfuhrmann.outfit.inventory.api.rest;

import github.io.ddmfuhrmann.outfit.inventory.application.dto.ManualAdjustmentRequest;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockMovementResponse;
import github.io.ddmfuhrmann.outfit.inventory.application.usecase.GetStockMovementsUseCase;
import github.io.ddmfuhrmann.outfit.inventory.application.usecase.RecordManualAdjustmentUseCase;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final RecordManualAdjustmentUseCase recordManualAdjustment;
    private final GetStockMovementsUseCase getStockMovements;

    public InventoryController(RecordManualAdjustmentUseCase recordManualAdjustment,
                               GetStockMovementsUseCase getStockMovements) {
        this.recordManualAdjustment = recordManualAdjustment;
        this.getStockMovements = getStockMovements;
    }

    @PostMapping("/adjustment")
    ResponseEntity<Void> adjust(@RequestBody ManualAdjustmentRequest request) {
        recordManualAdjustment.execute(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movements/{skuId}")
    @Operation(summary = "Get stock movements for a SKU",
            description = "Served from PostgreSQL.")
    ResponseEntity<PageResponse<StockMovementResponse>> movements(
            @PathVariable Long skuId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(getStockMovements.execute(skuId, pageable));
    }
}
