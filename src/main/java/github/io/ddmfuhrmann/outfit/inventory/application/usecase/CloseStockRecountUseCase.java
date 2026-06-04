package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.StockMovementService;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockBalance;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockRecountItem;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockBalanceRepository;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockEntryRepository;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockRecountRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@Transactional
public class CloseStockRecountUseCase {

    private final StockRecountRepository recountRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockEntryRepository entryRepository;
    private final StockMovementService stockMovementService;
    private final Clock clock;

    public CloseStockRecountUseCase(StockRecountRepository recountRepository,
                                    StockBalanceRepository balanceRepository,
                                    StockEntryRepository entryRepository,
                                    StockMovementService stockMovementService,
                                    Clock clock) {
        this.recountRepository = recountRepository;
        this.balanceRepository = balanceRepository;
        this.entryRepository = entryRepository;
        this.stockMovementService = stockMovementService;
        this.clock = clock;
    }

    public void execute(Long recountId) {
        var recount = recountRepository.findById(recountId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock recount not found: " + recountId));

        Instant now = Instant.now(clock);
        var items = recount.close(now);
        var skuIds = items.stream().map(StockRecountItem::getProductSkuId).toList();

        var balancesBySkuId = fetchBalances(skuIds);
        var productIdBySkuId = fetchProductIds(skuIds);

        applyAdjustments(items, balancesBySkuId, productIdBySkuId, recount.getId(), now);

        recountRepository.save(recount);
        log.info("Stock recount closed: id={}", recountId);
    }

    private Map<Long, StockBalance> fetchBalances(List<Long> skuIds) {
        return balanceRepository.findAllById(skuIds).stream()
                .collect(toMap(StockBalance::getProductSkuId, identity()));
    }

    private Map<Long, Long> fetchProductIds(List<Long> skuIds) {
        return entryRepository.findProductIdsBySkuIds(skuIds).stream()
                .collect(toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private void applyAdjustments(List<StockRecountItem> items,
                                   Map<Long, StockBalance> balancesBySkuId,
                                   Map<Long, Long> productIdBySkuId,
                                   Long recountId, Instant now) {
        for (var item : items) {
            var balance = balancesBySkuId.get(item.getProductSkuId());
            if (balance == null) continue;

            item.recordSystemBalance(balance.getCurrentBalance());

            int delta = item.getCountedQty() - balance.getCurrentBalance();
            if (delta != 0) {
                recordAdjustment(item, productIdBySkuId, delta, recountId, now);
            }
        }
    }

    private void recordAdjustment(StockRecountItem item, Map<Long, Long> productIdBySkuId,
                                   int delta, Long recountId, Instant now) {
        var productId = productIdBySkuId.get(item.getProductSkuId());
        if (productId == null) return;
        stockMovementService.recordEntry(item.getProductSkuId(), productId, delta,
                StockSource.RECOUNT_ADJUSTMENT, recountId, now);
    }
}
