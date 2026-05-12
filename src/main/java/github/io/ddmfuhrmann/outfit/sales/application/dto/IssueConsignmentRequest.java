package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record IssueConsignmentRequest(
        @NotNull Long customerId,
        @NotEmpty List<Long> salespersonIds,
        @NotNull LocalDate issueDate,
        String notes,
        @NotEmpty List<ConsignmentItemRequest> items
) {}
