package github.io.ddmfuhrmann.outfit.purchasing.application.usecase;

import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.domain.repository.PurchaseRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
public class AddPayableUseCase {

    private final PurchaseRepository repository;

    public AddPayableUseCase(PurchaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PurchaseResponse execute(Long purchaseId, LocalDate dueDate, BigDecimal amount) {
        var purchase = repository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + purchaseId));
        purchase.addPayable(dueDate, amount);
        var saved = repository.save(purchase);
        log.info("Added payable to purchase {}", purchaseId);
        return PurchaseResponse.from(saved);
    }
}
