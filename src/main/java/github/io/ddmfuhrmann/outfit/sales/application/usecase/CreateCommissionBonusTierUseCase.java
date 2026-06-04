package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CommissionBonusTierResponse;
import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateCommissionBonusTierRequest;
import github.io.ddmfuhrmann.outfit.sales.domain.model.CommissionBonusTier;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.CommissionBonusTierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CreateCommissionBonusTierUseCase {

    private final CommissionBonusTierRepository repository;

    public CreateCommissionBonusTierUseCase(CommissionBonusTierRepository repository) {
        this.repository = repository;
    }

    public CommissionBonusTierResponse execute(CreateCommissionBonusTierRequest request) {
        boolean overlaps = repository.existsOverlappingActiveTier(
                request.minAmount(), request.maxAmount(), null);
        if (overlaps) {
            throw new IllegalStateException("An active bonus tier already exists within that amount range");
        }

        var tier = CommissionBonusTier.create(request.minAmount(), request.maxAmount(), request.bonusPercent());
        repository.save(tier);
        log.info("CommissionBonusTier {} created", tier.getId());
        return CommissionBonusTierResponse.from(tier);
    }
}
