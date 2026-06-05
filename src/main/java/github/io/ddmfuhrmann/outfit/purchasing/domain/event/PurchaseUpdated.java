package github.io.ddmfuhrmann.outfit.purchasing.domain.event;

import java.time.LocalDate;
import java.util.List;

public record PurchaseUpdated(
        Long purchaseId,
        Long brandId,
        Long supplierId,
        LocalDate purchaseDate,
        String observations,
        String status,
        List<PurchaseLineSnapshot> lines,
        List<PurchasePayableSnapshot> payables) {}
