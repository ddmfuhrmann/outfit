package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditNote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StoreCreditNoteResponse(
        Long id,
        Long customerId,
        String status,
        BigDecimal totalAmount,
        Long consumedBySaleId,
        String notes,
        List<StoreCreditItemResponse> items,
        Instant createdAt) {

    public static StoreCreditNoteResponse from(StoreCreditNote note) {
        return new StoreCreditNoteResponse(
                note.getId(),
                note.getCustomerId(),
                note.getStatus().name(),
                note.getTotalAmount(),
                note.getConsumedBySaleId(),
                note.getNotes(),
                note.getItems().stream().map(StoreCreditItemResponse::from).toList(),
                note.getCreatedAt());
    }
}
