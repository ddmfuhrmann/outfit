package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockMovementResponse;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockEntryRepository;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GetStockMovementsUseCase {

    private final StockEntryRepository entryRepository;

    public GetStockMovementsUseCase(StockEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    public PageResponse<StockMovementResponse> execute(Long skuId, Pageable pageable) {
        var page = entryRepository.findByProductSkuIdOrderByOccurredAtDesc(skuId, pageable)
                .map(StockMovementResponse::from);
        return PageResponse.from(page);
    }
}
