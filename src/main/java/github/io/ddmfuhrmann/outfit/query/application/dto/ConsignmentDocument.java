package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ConsignmentDocument(
        Long consignmentId,
        String status,
        LocalDate issueDate,
        Instant closedAt,
        String customerName,
        ConsignmentCustomerDocument customer,
        List<ConsignmentSellerDocument> sellers,
        List<ConsignmentItemDocument> items,
        Instant updatedAt) {}
