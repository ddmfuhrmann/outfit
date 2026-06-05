package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import github.io.ddmfuhrmann.outfit.party.application.SupplierVerificationService;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AddSupplierToBrandUseCase {

    private final BrandRepository brandRepository;
    private final SupplierVerificationService supplierVerificationService;

    public AddSupplierToBrandUseCase(BrandRepository brandRepository,
                                     SupplierVerificationService supplierVerificationService) {
        this.brandRepository = brandRepository;
        this.supplierVerificationService = supplierVerificationService;
    }

    @Transactional
    public BrandResponse execute(Long brandId, Long supplierId) {
        var brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + brandId));
        supplierVerificationService.verifyIsSupplier(supplierId);
        brand.addSupplier(supplierId);
        brandRepository.save(brand);
        log.info("Supplier added to brand: brandId={}, supplierId={}", brandId, supplierId);
        return BrandResponse.from(brand);
    }
}
