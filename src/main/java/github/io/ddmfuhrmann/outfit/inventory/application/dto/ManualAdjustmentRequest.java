package github.io.ddmfuhrmann.outfit.inventory.application.dto;

import java.time.Instant;

public record ManualAdjustmentRequest(Long skuId, int desiredBalance, Instant occurredAt) {}
