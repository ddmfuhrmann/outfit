package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SizeRequest(@NotBlank String description) {}
