package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RemoveSupplierFromBrandUseCase {

    private final BrandRepository brandRepository;

    public RemoveSupplierFromBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public BrandResponse execute(Long brandId, Long supplierId) {
        var brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + brandId));
        brand.removeSupplier(supplierId);
        brandRepository.save(brand);
        log.info("Supplier removed from brand: brandId={}, supplierId={}", brandId, supplierId);
        return BrandResponse.from(brand);
    }
}
