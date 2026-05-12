package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.util.List;

public record ConsignmentIssued(
        Long consignmentId,
        Long customerId,
        List<Long> salespersonIds,
        List<ConsignmentItemSnapshot> items) {}
