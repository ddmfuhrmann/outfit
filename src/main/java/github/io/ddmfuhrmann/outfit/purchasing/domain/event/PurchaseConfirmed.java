package github.io.ddmfuhrmann.outfit.purchasing.domain.event;

import java.time.LocalDate;
import java.util.List;

public record PurchaseConfirmed(
        Long purchaseId,
        Long brandId,
        Long supplierId,
        LocalDate purchaseDate,
        String observations,
        List<PurchaseLineSnapshot> lines,
        List<PurchasePayableSnapshot> payables) {}
