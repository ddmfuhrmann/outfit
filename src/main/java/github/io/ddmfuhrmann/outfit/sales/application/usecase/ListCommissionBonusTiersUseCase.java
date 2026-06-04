package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CommissionBonusTierResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.CommissionBonusTierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListCommissionBonusTiersUseCase {

    private final CommissionBonusTierRepository repository;

    public ListCommissionBonusTiersUseCase(CommissionBonusTierRepository repository) {
        this.repository = repository;
    }

    public List<CommissionBonusTierResponse> execute() {
        return repository.findByActiveTrue().stream()
                .map(CommissionBonusTierResponse::from)
                .toList();
    }
}
