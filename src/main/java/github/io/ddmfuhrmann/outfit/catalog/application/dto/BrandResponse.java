package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Brand;

import java.time.Instant;
import java.util.List;

public record BrandResponse(Long id, String description, Instant createdAt, List<Long> supplierIds) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getDescription(),
                brand.getCreatedAt(),
                List.copyOf(brand.getSupplierIds()));
    }
}
