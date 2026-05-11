package github.io.ddmfuhrmann.outfit.inventory.api.rest;

import github.io.ddmfuhrmann.outfit.inventory.application.dto.AddRecountItemRequest;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.OpenStockRecountRequest;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockRecountResponse;
import github.io.ddmfuhrmann.outfit.inventory.application.usecase.AddRecountItemUseCase;
import github.io.ddmfuhrmann.outfit.inventory.application.usecase.CloseStockRecountUseCase;
import github.io.ddmfuhrmann.outfit.inventory.application.usecase.GetStockRecountUseCase;
import github.io.ddmfuhrmann.outfit.inventory.application.usecase.OpenStockRecountUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/inventory/recount")
public class StockRecountController {

    private final OpenStockRecountUseCase openRecount;
    private final AddRecountItemUseCase addRecountItem;
    private final CloseStockRecountUseCase closeRecount;
    private final GetStockRecountUseCase getRecount;

    public StockRecountController(OpenStockRecountUseCase openRecount,
                                  AddRecountItemUseCase addRecountItem,
                                  CloseStockRecountUseCase closeRecount,
                                  GetStockRecountUseCase getRecount) {
        this.openRecount = openRecount;
        this.addRecountItem = addRecountItem;
        this.closeRecount = closeRecount;
        this.getRecount = getRecount;
    }

    @PostMapping
    ResponseEntity<Map<String, Long>> open(@RequestBody OpenStockRecountRequest request) {
        Long id = openRecount.execute(request);
        return ResponseEntity.status(201).body(Map.of("id", id));
    }

    @PostMapping("/{id}/items")
    ResponseEntity<Void> addItem(@PathVariable Long id, @RequestBody AddRecountItemRequest request) {
        addRecountItem.execute(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    ResponseEntity<Void> close(@PathVariable Long id) {
        closeRecount.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    ResponseEntity<StockRecountResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getRecount.execute(id));
    }
}
