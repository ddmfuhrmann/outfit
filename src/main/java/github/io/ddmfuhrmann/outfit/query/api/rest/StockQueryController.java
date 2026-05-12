package github.io.ddmfuhrmann.outfit.query.api.rest;

import github.io.ddmfuhrmann.outfit.query.application.dto.BulkBalanceRequest;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockMonthlyDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.StockSnapshotDocument;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetStockMonthlyUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetStockSnapshotBulkUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.GetStockSnapshotUseCase;
import github.io.ddmfuhrmann.outfit.query.application.usecase.SearchStockSnapshotUseCase;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class StockQueryController {

    private final GetStockSnapshotUseCase getStockSnapshot;
    private final SearchStockSnapshotUseCase searchStockSnapshot;
    private final GetStockSnapshotBulkUseCase getStockSnapshotBulk;
    private final GetStockMonthlyUseCase getStockMonthly;

    public StockQueryController(GetStockSnapshotUseCase getStockSnapshot,
                                SearchStockSnapshotUseCase searchStockSnapshot,
                                GetStockSnapshotBulkUseCase getStockSnapshotBulk,
                                GetStockMonthlyUseCase getStockMonthly) {
        this.getStockSnapshot = getStockSnapshot;
        this.searchStockSnapshot = searchStockSnapshot;
        this.getStockSnapshotBulk = getStockSnapshotBulk;
        this.getStockMonthly = getStockMonthly;
    }

    @GetMapping("/balance/{skuId}")
    @Operation(summary = "Get current stock balance for a SKU",
            description = "Served from Elasticsearch. Eventually consistent — reflects the state of the index after domain events are processed.")
    ResponseEntity<StockSnapshotDocument> getBalance(@PathVariable Long skuId) {
        return ResponseEntity.ok(getStockSnapshot.execute(skuId));
    }

    @GetMapping("/balance")
    @Operation(summary = "Search stock balances",
            description = "Served from Elasticsearch. Eventually consistent — reflects the state of the index after domain events are processed.")
    ResponseEntity<PageResponse<StockSnapshotDocument>> searchBalances(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(searchStockSnapshot.execute(productId, brandId, categoryId, pageable));
    }

    @PostMapping("/balance/bulk")
    @Operation(summary = "Bulk fetch stock balances by SKU IDs",
            description = "Served from Elasticsearch. Eventually consistent — reflects the state of the index after domain events are processed.")
    ResponseEntity<List<StockSnapshotDocument>> bulkBalances(@RequestBody BulkBalanceRequest request) {
        return ResponseEntity.ok(getStockSnapshotBulk.execute(request.skuIds()));
    }

    @GetMapping("/stock/monthly")
    @Operation(summary = "Get monthly stock history",
            description = "Served from Elasticsearch. Eventually consistent — reflects the state of the index after domain events are processed.")
    ResponseEntity<PageResponse<StockMonthlyDocument>> getMonthly(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String yearMonth,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(getStockMonthly.execute(skuId, productId, brandId, categoryId, yearMonth, pageable));
    }
}
