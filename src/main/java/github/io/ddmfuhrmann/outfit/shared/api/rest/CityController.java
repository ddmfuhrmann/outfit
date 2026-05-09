package github.io.ddmfuhrmann.outfit.shared.api.rest;

import github.io.ddmfuhrmann.outfit.shared.application.dto.CityResponse;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import github.io.ddmfuhrmann.outfit.shared.application.usecase.GetCityUseCase;
import github.io.ddmfuhrmann.outfit.shared.application.usecase.ListCitiesUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shared/cities")
public class CityController {

    private final ListCitiesUseCase listCities;
    private final GetCityUseCase getCity;

    public CityController(ListCitiesUseCase listCities, GetCityUseCase getCity) {
        this.listCities = listCities;
        this.getCity = getCity;
    }

    @GetMapping
    ResponseEntity<PageResponse<CityResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(listCities.execute(search, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    ResponseEntity<CityResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(getCity.execute(id));
    }
}
