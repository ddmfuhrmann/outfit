package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleInstallment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleInstallmentResponse(
        Long id,
        String paymentModality,
        LocalDate dueDate,
        BigDecimal amount) {

    public static SaleInstallmentResponse from(SaleInstallment installment) {
        return new SaleInstallmentResponse(
                installment.getId(),
                installment.getPaymentModality().name(),
                installment.getDueDate(),
                installment.getAmount());
    }
}
