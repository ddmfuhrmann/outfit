package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateProductRequest(
        @NotBlank String description,
        @NotNull BigDecimal price,
        @NotNull BigDecimal cost,
        LocalDate purchaseDate,
        Long colorId,
        @NotNull Long brandId,
        @NotNull Long categoryId,
        @NotNull @Valid List<CreateSkuRequest> skus
) {}
