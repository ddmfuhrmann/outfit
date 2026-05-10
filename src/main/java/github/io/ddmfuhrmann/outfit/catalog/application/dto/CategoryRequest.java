package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String description, String ncmCode) {}
