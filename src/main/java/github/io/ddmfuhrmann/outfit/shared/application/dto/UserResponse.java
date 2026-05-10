package github.io.ddmfuhrmann.outfit.shared.application.dto;

import github.io.ddmfuhrmann.outfit.shared.domain.model.User;
import github.io.ddmfuhrmann.outfit.shared.domain.model.UserRole;

import java.time.Instant;

public record UserResponse(
        String login,
        String name,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getLogin(),
                user.getName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
