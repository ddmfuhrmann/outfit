package github.io.ddmfuhrmann.outfit.party.domain.event;

public record PartyAddressSnapshot(
        Long id,
        String street,
        String neighborhood,
        String zipCode,
        String number,
        String complement,
        Integer cityIbgeCode) {}
