package github.io.ddmfuhrmann.outfit.query.application.dto;

public record AddressDocument(Long id, String street, String neighborhood, String zipCode,
                              String number, String complement, Long cityId, String cityName, String stateAbbr) {}
