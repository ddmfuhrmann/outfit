package github.io.ddmfuhrmann.outfit.shared.domain.event;

import github.io.ddmfuhrmann.outfit.shared.domain.model.UserRole;

public record UserCreated(String login, String name, UserRole role) {}
