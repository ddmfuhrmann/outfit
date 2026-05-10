package github.io.ddmfuhrmann.outfit.shared.application.usecase;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CityResponse;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import github.io.ddmfuhrmann.outfit.shared.domain.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCityUseCase {

    private final CityRepository cityRepository;

    public GetCityUseCase(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public CityResponse execute(Integer ibgeCode) {
        return cityRepository.findById(ibgeCode)
                .map(CityResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("City " + ibgeCode + " not found"));
    }
}
