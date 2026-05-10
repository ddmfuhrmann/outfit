package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.SizeRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSizeUseCase {

    private final SizeRepository sizeRepository;

    public GetSizeUseCase(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    @Transactional(readOnly = true)
    public SizeResponse execute(Long id) {
        return sizeRepository.findById(id)
                .map(SizeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Size not found: " + id));
    }
}
