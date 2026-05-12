package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.Consignment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ConsignmentResponse(
        Long id,
        Long customerId,
        List<Long> salespersonIds,
        String status,
        LocalDate issueDate,
        Instant closedAt,
        String notes,
        List<ConsignmentItemResponse> items
) {
    public static ConsignmentResponse from(Consignment c) {
        return new ConsignmentResponse(
                c.getId(),
                c.getCustomerId(),
                List.copyOf(c.getSalespersonIds()),
                c.getStatus().name(),
                c.getIssueDate(),
                c.getClosedAt(),
                c.getNotes(),
                c.getItems().stream().map(ConsignmentItemResponse::from).toList()
        );
    }
}
