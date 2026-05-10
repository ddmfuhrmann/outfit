package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.SizeResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.SizeRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RenameSizeUseCase {

    private final SizeRepository sizeRepository;

    public RenameSizeUseCase(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    @Transactional
    public SizeResponse execute(Long id, SizeRequest request) {
        var size = sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Size not found: " + id));
        size.rename(request.description());
        sizeRepository.save(size);
        log.info("Size renamed: id={}, description={}", id, request.description());
        return SizeResponse.from(size);
    }
}
