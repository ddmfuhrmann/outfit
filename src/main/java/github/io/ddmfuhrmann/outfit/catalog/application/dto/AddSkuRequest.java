package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddSkuRequest(
        @NotBlank String barcode,
        @NotNull Long sizeId,
        int implantationQty
) {}
