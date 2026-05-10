package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ColorRequest(@NotBlank String description) {}
