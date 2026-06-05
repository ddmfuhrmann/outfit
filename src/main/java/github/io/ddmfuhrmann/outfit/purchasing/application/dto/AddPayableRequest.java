package github.io.ddmfuhrmann.outfit.purchasing.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPayableRequest(
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {}
