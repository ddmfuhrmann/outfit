package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.model.Color;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ColorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CreateColorUseCase {

    private final ColorRepository colorRepository;

    public CreateColorUseCase(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Transactional
    public ColorResponse execute(ColorRequest request) {
        var color = Color.create(request.description());
        var response = ColorResponse.from(colorRepository.save(color));
        log.info("Color created: id={}, description={}", response.id(), response.description());
        return response;
    }
}
