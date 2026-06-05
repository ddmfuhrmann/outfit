package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseDocument(
        Long id,
        Long brandId,
        Long supplierId,
        String supplierName,
        LocalDate purchaseDate,
        String observations,
        String status,
        BigDecimal totalCost,
        List<PurchaseLineDocument> lines,
        List<PurchasePayableDocument> payables,
        Instant indexedAt) {}
