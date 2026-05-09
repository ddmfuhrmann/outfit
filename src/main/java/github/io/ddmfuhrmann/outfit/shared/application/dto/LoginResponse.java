package github.io.ddmfuhrmann.outfit.shared.application.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt) {
}
