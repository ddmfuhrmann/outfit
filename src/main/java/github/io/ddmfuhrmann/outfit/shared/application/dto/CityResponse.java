package github.io.ddmfuhrmann.outfit.shared.application.dto;

import github.io.ddmfuhrmann.outfit.shared.domain.model.City;

import java.time.Instant;

public record CityResponse(
        Integer ibgeCityCode,
        Integer ibgeStateCode,
        String cityName,
        String stateName,
        String stateAbbr,
        Instant createdAt,
        Instant updatedAt
) {
    public static CityResponse from(City city) {
        return new CityResponse(
                city.getIbgeCityCode(),
                city.getIbgeStateCode(),
                city.getCityName(),
                city.getStateName(),
                city.getStateAbbr(),
                city.getCreatedAt(),
                city.getUpdatedAt()
        );
    }
}
