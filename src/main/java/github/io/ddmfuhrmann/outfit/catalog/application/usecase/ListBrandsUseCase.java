package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.BrandRepository;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListBrandsUseCase {

    private final BrandRepository brandRepository;

    public ListBrandsUseCase(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> execute(Pageable pageable) {
        return PageResponse.from(brandRepository.findAll(pageable).map(BrandResponse::from));
    }
}
