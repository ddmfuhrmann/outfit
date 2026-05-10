package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Size;

import java.time.Instant;

public record SizeResponse(Long id, String description, Instant createdAt) {
    public static SizeResponse from(Size size) {
        return new SizeResponse(size.getId(), size.getDescription(), size.getCreatedAt());
    }
}
