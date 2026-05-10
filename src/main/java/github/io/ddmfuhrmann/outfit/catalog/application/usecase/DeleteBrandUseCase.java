package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ProductRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeleteBrandUseCase {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public DeleteBrandUseCase(BrandRepository brandRepository, ProductRepository productRepository) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand not found: " + id);
        }
        if (productRepository.existsByBrandId(id)) {
            throw new IllegalStateException("Brand is in use by one or more products");
        }
        brandRepository.deleteById(id);
        log.info("Brand deleted: id={}", id);
    }
}
