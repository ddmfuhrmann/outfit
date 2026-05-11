package github.io.ddmfuhrmann.outfit.inventory.application.usecase;

import github.io.ddmfuhrmann.outfit.inventory.application.dto.OpenStockRecountRequest;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockRecount;
import github.io.ddmfuhrmann.outfit.inventory.domain.repository.StockRecountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class OpenStockRecountUseCase {

    private final StockRecountRepository recountRepository;

    public OpenStockRecountUseCase(StockRecountRepository recountRepository) {
        this.recountRepository = recountRepository;
    }

    public Long execute(OpenStockRecountRequest request) {
        if (request.date() == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        var recount = StockRecount.create(request.date(), request.notes());
        recountRepository.save(recount);
        log.info("Stock recount opened: id={}, date={}", recount.getId(), recount.getDate());
        return recount.getId();
    }
}
