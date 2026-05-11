package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.dto.AddRecountItemRequest;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockRecountRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AddRecountItemUseCase {

    private final StockRecountRepository recountRepository;

    public AddRecountItemUseCase(StockRecountRepository recountRepository) {
        this.recountRepository = recountRepository;
    }

    public void execute(Long recountId, AddRecountItemRequest request) {
        var recount = recountRepository.findById(recountId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock recount not found: " + recountId));
        recount.addItem(request.skuId(), request.countedQty());
        log.info("Item added to recount {}: skuId={}", recountId, request.skuId());
    }
}
