package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockBalance;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class RecordInitialStockUseCase {

    private final StockMovementService movementService;
    private final StockBalanceRepository balanceRepository;

    public RecordInitialStockUseCase(StockMovementService movementService,
                                     StockBalanceRepository balanceRepository) {
        this.movementService = movementService;
        this.balanceRepository = balanceRepository;
    }

    public void execute(Long skuId, Long productId, int implantationQty, Instant occurredAt) {
        if (implantationQty < 0) {
            throw new IllegalArgumentException("implantationQty must not be negative");
        }
        if (implantationQty == 0) {
            balanceRepository.save(StockBalance.create(skuId));
            return;
        }
        movementService.recordEntry(skuId, productId, implantationQty,
                StockSource.INITIAL_STOCK, skuId, occurredAt);
    }
}
