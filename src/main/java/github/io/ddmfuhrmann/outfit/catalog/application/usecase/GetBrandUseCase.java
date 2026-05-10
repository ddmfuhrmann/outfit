package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBrandUseCase {

    private final BrandRepository brandRepository;

    public GetBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public BrandResponse execute(Long id) {
        return brandRepository.findById(id)
                .map(BrandResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + id));
    }
}
