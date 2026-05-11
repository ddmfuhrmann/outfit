package github.io.ddmfuhrmann.outfit.inventory.application.dto;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockRecount;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockRecountStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StockRecountResponse(
        Long id,
        LocalDate date,
        String notes,
        StockRecountStatus status,
        Instant closedAt,
        List<RecountItemResponse> items) {

    public static StockRecountResponse from(StockRecount r) {
        var items = r.getItems().stream().map(RecountItemResponse::from).toList();
        return new StockRecountResponse(r.getId(), r.getDate(), r.getNotes(), r.getStatus(), r.getClosedAt(), items);
    }
}
