package github.io.ddmfuhrmann.outfit.inventory.application;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockBalance;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockEntry;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockBalanceRepository;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class StockMovementService {

    private final StockBalanceRepository balanceRepository;
    private final StockEntryRepository entryRepository;

    public StockMovementService(StockBalanceRepository balanceRepository,
                                StockEntryRepository entryRepository) {
        this.balanceRepository = balanceRepository;
        this.entryRepository = entryRepository;
    }

    public StockEntry recordEntry(Long skuId, Long productId, int quantity,
                             StockSource source, Long sourceKey, Instant occurredAt) {
        var balance = balanceRepository.findAndLock(skuId)
                .orElseGet(() -> StockBalance.create(skuId));

        var entry = StockEntry.create(skuId, productId, quantity, balance.getCurrentBalance(),
                source, sourceKey, occurredAt);
        entryRepository.save(entry);

        balance.apply(quantity);
        balanceRepository.save(balance);

        return entry;
    }
}
