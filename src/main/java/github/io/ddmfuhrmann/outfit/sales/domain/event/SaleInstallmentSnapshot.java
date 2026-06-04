package github.io.ddmfuhrmann.outfit.sales.domain.event;

import github.io.ddmfuhrmann.outfit.sales.domain.model.PaymentModality;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleInstallmentSnapshot(
        String paymentModality,
        LocalDate dueDate,
        BigDecimal amount) {

    public boolean isDeferred() {
        return PaymentModality.valueOf(paymentModality).isDeferred();
    }
}
