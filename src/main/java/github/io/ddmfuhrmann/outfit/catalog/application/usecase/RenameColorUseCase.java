package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ColorRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RenameColorUseCase {

    private final ColorRepository colorRepository;

    public RenameColorUseCase(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Transactional
    public ColorResponse execute(Long id, ColorRequest request) {
        var color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Color not found: " + id));
        color.rename(request.description());
        log.info("Color renamed: id={}, description={}", id, request.description());
        return ColorResponse.from(color);
    }
}
