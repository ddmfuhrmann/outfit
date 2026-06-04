package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleInstallmentDocument(
        String paymentModality,
        LocalDate dueDate,
        BigDecimal amount) {}
