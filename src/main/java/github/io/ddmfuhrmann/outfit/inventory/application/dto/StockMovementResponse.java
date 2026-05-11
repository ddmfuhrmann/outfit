package github.io.ddmfuhrmann.outfit.inventory.application.dto;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockEntry;
import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;

import java.time.Instant;

public record StockMovementResponse(
        Long id, int quantity, int runningBalance,
        StockSource source, Long sourceKey, Instant occurredAt) {

    public static StockMovementResponse from(StockEntry e) {
        return new StockMovementResponse(
                e.getId(), e.getQuantity(), e.getRunningBalance(),
                e.getSource(), e.getSourceKey(), e.getOccurredAt());
    }
}
