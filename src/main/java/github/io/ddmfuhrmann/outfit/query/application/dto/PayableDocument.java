package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PayableDocument(
        Long payableId,
        Long purchaseId,
        Long supplierId,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal balance,
        String status,
        Instant createdAt) {}
