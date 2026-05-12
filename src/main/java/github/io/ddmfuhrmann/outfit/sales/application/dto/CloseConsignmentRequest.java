package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CloseConsignmentRequest(
        @NotEmpty List<Long> sellerIds,
        @NotEmpty List<CreateSaleInstallmentRequest> installments
) {}
