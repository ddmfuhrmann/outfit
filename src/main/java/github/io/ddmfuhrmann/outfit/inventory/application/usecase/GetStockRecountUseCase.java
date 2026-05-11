package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.dto.StockRecountResponse;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockRecountRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetStockRecountUseCase {

    private final StockRecountRepository recountRepository;

    public GetStockRecountUseCase(StockRecountRepository recountRepository) {
        this.recountRepository = recountRepository;
    }

    public StockRecountResponse execute(Long recountId) {
        var recount = recountRepository.findById(recountId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock recount not found: " + recountId));
        return StockRecountResponse.from(recount);
    }
}
