package github.io.ddmfuhrmann.outfit.purchasing.application.usecase;

import github.io.ddmfuhrmann.outfit.purchasing.domain.repository.PurchaseRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RemovePayableUseCase {

    private final PurchaseRepository repository;

    public RemovePayableUseCase(PurchaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long purchaseId, Long payableId) {
        var purchase = repository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + purchaseId));
        purchase.removePayable(payableId);
        repository.save(purchase);
        log.info("Removed payable {} from purchase {}", payableId, purchaseId);
    }
}
