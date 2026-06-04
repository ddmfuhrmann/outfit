package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CommissionBonusTierResponse;
import github.io.ddmfuhrmann.outfit.sales.application.dto.UpdateCommissionBonusTierRequest;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.CommissionBonusTierRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UpdateCommissionBonusTierUseCase {

    private final CommissionBonusTierRepository repository;

    public UpdateCommissionBonusTierUseCase(CommissionBonusTierRepository repository) {
        this.repository = repository;
    }

    public CommissionBonusTierResponse execute(Long id, UpdateCommissionBonusTierRequest request) {
        var tier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommissionBonusTier not found: " + id));

        boolean overlaps = repository.existsOverlappingActiveTier(
                request.minAmount(), request.maxAmount(), id);
        if (overlaps) {
            throw new IllegalStateException("An active bonus tier already exists within that amount range");
        }

        tier.update(request.minAmount(), request.maxAmount(), request.bonusPercent());
        repository.save(tier);
        log.info("CommissionBonusTier {} updated", id);
        return CommissionBonusTierResponse.from(tier);
    }
}
