package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.PaymentModality;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSaleInstallmentRequest(
        @NotNull PaymentModality paymentModality,
        @NotNull LocalDate dueDate,
        @NotNull @Positive BigDecimal amount) {}
