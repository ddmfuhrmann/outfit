package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Category;

import java.time.Instant;

public record CategoryResponse(Long id, String description, String ncmCode, Instant createdAt) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getDescription(), category.getNcmCode(), category.getCreatedAt());
    }
}
