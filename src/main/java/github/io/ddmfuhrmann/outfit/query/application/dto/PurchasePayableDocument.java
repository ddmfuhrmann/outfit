package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchasePayableDocument(Long id, LocalDate dueDate, BigDecimal amount) {}
