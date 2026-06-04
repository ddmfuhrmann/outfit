package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateStoreCreditNoteRequest(
        @NotNull Long customerId,
        String notes,
        @NotEmpty @Valid List<StoreCreditItemRequest> items) {}
