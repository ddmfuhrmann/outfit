package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.application.dto.ManualAdjustmentRequest;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockBalanceRepository;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockEntryRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class RecordManualAdjustmentUseCase {

    private final StockBalanceRepository balanceRepository;
    private final StockEntryRepository entryRepository;
    private final StockMovementService stockMovementService;

    public RecordManualAdjustmentUseCase(StockBalanceRepository balanceRepository,
                                         StockEntryRepository entryRepository,
                                         StockMovementService stockMovementService) {
        this.balanceRepository = balanceRepository;
        this.entryRepository = entryRepository;
        this.stockMovementService = stockMovementService;
    }

    public void execute(ManualAdjustmentRequest request) {
        var balance = balanceRepository.findById(request.skuId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock balance not found for SKU " + request.skuId()));

        int quantity = request.desiredBalance() - balance.getCurrentBalance();
        if (quantity == 0) {
            throw new IllegalStateException("Desired balance equals current balance");
        }

        var productId = entryRepository.findProductIdByProductSkuId(request.skuId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No stock entries found for SKU " + request.skuId()));

        stockMovementService.recordEntry(request.skuId(), productId, quantity,
                StockSource.MANUAL_ADJUSTMENT, null, request.occurredAt());

        log.info("Manual adjustment recorded for SKU {}: desiredBalance={}", request.skuId(), request.desiredBalance());
    }
}
