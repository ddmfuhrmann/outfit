package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Brand;

import java.time.Instant;

public record BrandResponse(Long id, String description, Instant createdAt) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getDescription(), brand.getCreatedAt());
    }
}
