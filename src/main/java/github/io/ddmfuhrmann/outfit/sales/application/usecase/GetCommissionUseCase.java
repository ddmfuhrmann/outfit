package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.SellerCommissionResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.SellerCommissionRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetCommissionUseCase {

    private final SellerCommissionRepository repository;

    public GetCommissionUseCase(SellerCommissionRepository repository) {
        this.repository = repository;
    }

    public SellerCommissionResponse execute(Long id) {
        var commission = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commission not found: " + id));
        return SellerCommissionResponse.from(commission);
    }
}
