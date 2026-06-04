package github.io.ddmfuhrmann.outfit.party.application;

import java.math.BigDecimal;

public record SalespersonDetails(
        Long salespersonId,
        String displayName,
        BigDecimal commissionPercent) {}
