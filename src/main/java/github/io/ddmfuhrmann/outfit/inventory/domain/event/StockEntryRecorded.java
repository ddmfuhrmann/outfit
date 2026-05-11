package github.io.ddmfuhrmann.outfit.inventory.domain.event;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockSource;

import java.time.Instant;

public record StockEntryRecorded(
        Long entryId, Long skuId, Long productId,
        int quantity, int runningBalance,
        StockSource source, Long sourceKey,
        Instant occurredAt) {}
