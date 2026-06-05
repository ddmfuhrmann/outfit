package github.io.ddmfuhrmann.outfit.purchasing.application.usecase;

import github.io.ddmfuhrmann.outfit.purchasing.application.dto.PurchaseResponse;
import github.io.ddmfuhrmann.outfit.purchasing.domain.repository.PurchaseRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UpdateObservationsUseCase {

    private final PurchaseRepository repository;

    public UpdateObservationsUseCase(PurchaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PurchaseResponse execute(Long purchaseId, String observations) {
        var purchase = repository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + purchaseId));
        purchase.updateObservations(observations);
        var saved = repository.save(purchase);
        log.info("Updated observations for purchase {}", purchaseId);
        return PurchaseResponse.from(saved);
    }
}
