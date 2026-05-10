package github.io.ddmfuhrmann.outfit.party.application.dto;

import github.io.ddmfuhrmann.outfit.party.domain.model.ContactType;

public record AddContactRequest(ContactType classification, String description) {}
