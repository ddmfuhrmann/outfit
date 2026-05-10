package github.io.ddmfuhrmann.outfit.party.domain.event;

public record PartyContactSnapshot(
        Long id,
        String classification,
        String description) {}
