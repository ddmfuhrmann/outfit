package github.io.ddmfuhrmann.outfit.party.application.dto;

public record AddAddressRequest(
        String street,
        String neighborhood,
        String zipCode,
        String number,
        String complement,
        Integer cityIbgeCode
) {}
