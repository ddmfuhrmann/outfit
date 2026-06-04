package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.domain.repository.CommissionBonusTierRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class DeactivateCommissionBonusTierUseCase {

    private final CommissionBonusTierRepository repository;

    public DeactivateCommissionBonusTierUseCase(CommissionBonusTierRepository repository) {
        this.repository = repository;
    }

    public void execute(Long id) {
        var tier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommissionBonusTier not found: " + id));
        tier.deactivate();
        repository.save(tier);
        log.info("CommissionBonusTier {} deactivated", id);
    }
}
