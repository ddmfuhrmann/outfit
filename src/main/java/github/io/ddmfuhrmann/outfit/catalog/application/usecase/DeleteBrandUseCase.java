package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeleteBrandUseCase {

    private final BrandRepository brandRepository;

    public DeleteBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public void execute(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand not found: " + id);
        }
        brandRepository.deleteById(id);
        log.info("Brand deleted: id={}", id);
    }
}
