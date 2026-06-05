package github.io.ddmfuhrmann.outfit.purchasing.domain.event;

import java.time.LocalDate;

public record PurchaseOpened(
        Long purchaseId,
        Long brandId,
        Long supplierId,
        LocalDate purchaseDate,
        String observations) {}
