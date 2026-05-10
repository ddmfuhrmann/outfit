package github.io.ddmfuhrmann.outfit.party.domain.event;

public record PartyUpdated(Long partyId, PartySnapshot snapshot) {}
