package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.math.BigDecimal;
import java.util.List;

public record StoreCreditNoteCreated(
        Long storeCreditNoteId,
        Long customerId,
        BigDecimal totalAmount,
        List<StoreCreditItemSnapshot> items) {}
