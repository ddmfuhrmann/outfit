package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.domain.repository.SizeRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeleteSizeUseCase {

    private final SizeRepository sizeRepository;

    public DeleteSizeUseCase(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    @Transactional
    public void execute(Long id) {
        var size = sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Size not found: " + id));
        size.delete();
        sizeRepository.save(size);
        sizeRepository.delete(size);
        log.info("Size deleted: id={}", id);
    }
}
