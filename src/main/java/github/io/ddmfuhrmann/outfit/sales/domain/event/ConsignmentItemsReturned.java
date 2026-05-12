package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.util.List;

public record ConsignmentItemsReturned(
        Long consignmentId,
        List<ConsignmentItemSnapshot> returnedItems) {}
