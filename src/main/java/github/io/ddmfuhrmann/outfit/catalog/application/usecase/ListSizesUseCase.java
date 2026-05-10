package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.SizeRepository;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListSizesUseCase {

    private final SizeRepository sizeRepository;

    public ListSizesUseCase(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SizeResponse> execute(Pageable pageable) {
        return PageResponse.from(sizeRepository.findAll(pageable).map(SizeResponse::from));
    }
}
