package github.io.ddmfuhrmann.outfit.party.domain.event;

public record PartyCreated(Long partyId, String legalName,
                           boolean customer, boolean supplier, boolean salesperson) {}
