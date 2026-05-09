package github.io.ddmfuhrmann.outfit.shared.application.usecase;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CityResponse;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.CityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCitiesUseCase {

    private final CityRepository cityRepository;

    public ListCitiesUseCase(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CityResponse> execute(String search, Pageable pageable) {
        var page = (search == null || search.isBlank())
                ? cityRepository.findAll(pageable)
                : cityRepository.findByCityNameContainingIgnoreCase(search, pageable);
        return PageResponse.from(page.map(CityResponse::from));
    }
}
