package github.io.ddmfuhrmann.outfit.sales.domain.event;

public record StoreCreditNoteConsumed(
        Long storeCreditNoteId,
        Long customerId,
        Long saleId) {}
