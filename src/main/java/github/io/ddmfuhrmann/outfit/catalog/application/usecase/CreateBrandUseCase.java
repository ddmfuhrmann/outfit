package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.model.Brand;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CreateBrandUseCase {

    private final BrandRepository brandRepository;

    public CreateBrandUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public BrandResponse execute(BrandRequest request) {
        var brand = Brand.create(request.description());
        var response = BrandResponse.from(brandRepository.save(brand));
        log.info("Brand created: id={}, description={}", response.id(), response.description());
        return response;
    }
}
