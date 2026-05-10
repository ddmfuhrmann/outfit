package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ColorRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DeleteColorUseCase {

    private final ColorRepository colorRepository;

    public DeleteColorUseCase(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Transactional
    public void execute(Long id) {
        if (!colorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Color not found: " + id);
        }
        colorRepository.deleteById(id);
        log.info("Color deleted: id={}", id);
    }
}
