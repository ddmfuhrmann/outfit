package github.io.ddmfuhrmann.outfit.shared.application.dto;

import github.io.ddmfuhrmann.outfit.shared.domain.model.UserRole;

public record UpdateUserRequest(
        String name,
        UserRole role
) {
}
