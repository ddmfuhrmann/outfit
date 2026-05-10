package github.io.ddmfuhrmann.outfit.query.application.dto;

public record AddressDocument(Long id, String street, String neighborhood, String zipCode,
                              String number, String complement, Integer cityIbgeCode, String cityName, String stateAbbr) {}
