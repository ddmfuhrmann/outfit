package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ConsignmentClosed(
        Long consignmentId,
        Long customerId,
        List<Long> salespersonIds,
        LocalDate issueDate,
        Instant closedAt,
        List<ConsignmentItemSnapshot> soldItems) {}
