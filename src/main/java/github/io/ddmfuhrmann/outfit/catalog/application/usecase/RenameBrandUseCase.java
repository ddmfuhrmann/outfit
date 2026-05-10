package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RenameBrandUseCase {

    private final BrandRepository brandRepository;

    public RenameBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public BrandResponse execute(Long id, BrandRequest request) {
        var brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + id));
        brand.rename(request.description());
        brandRepository.save(brand);
        log.info("Brand renamed: id={}, description={}", id, request.description());
        return BrandResponse.from(brand);
    }
}
