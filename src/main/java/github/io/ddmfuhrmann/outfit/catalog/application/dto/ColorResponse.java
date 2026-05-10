package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Color;

import java.time.Instant;

public record ColorResponse(Long id, String description, Instant createdAt) {
    public static ColorResponse from(Color color) {
        return new ColorResponse(color.getId(), color.getDescription(), color.getCreatedAt());
    }
}
