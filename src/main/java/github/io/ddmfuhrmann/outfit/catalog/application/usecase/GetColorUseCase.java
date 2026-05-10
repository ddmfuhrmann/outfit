package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ColorRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetColorUseCase {

    private final ColorRepository colorRepository;

    public GetColorUseCase(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Transactional(readOnly = true)
    public ColorResponse execute(Long id) {
        return colorRepository.findById(id)
                .map(ColorResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Color not found: " + id));
    }
}
