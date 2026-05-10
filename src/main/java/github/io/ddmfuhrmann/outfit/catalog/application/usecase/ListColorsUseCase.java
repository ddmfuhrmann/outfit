package github.io.ddmfuhrmann.outfit.catalog.application.usecase;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.ColorResponse;
import github.io.ddmfuhrmann.outfit.catalog.domain.repository.ColorRepository;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListColorsUseCase {

    private final ColorRepository colorRepository;

    public ListColorsUseCase(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ColorResponse> execute(Pageable pageable) {
        return PageResponse.from(colorRepository.findAll(pageable).map(ColorResponse::from));
    }
}
