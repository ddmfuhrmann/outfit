package github.io.ddmfuhrmann.outfit.shared.application.dto;

import github.io.ddmfuhrmann.outfit.shared.domain.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String login,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull UserRole role
) {
}
