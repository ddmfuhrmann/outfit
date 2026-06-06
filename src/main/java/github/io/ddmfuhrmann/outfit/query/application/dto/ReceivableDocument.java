package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReceivableDocument(
        Long receivableId,
        Long saleId,
        Long customerId,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal balance,
        String status,
        Instant createdAt) {}
