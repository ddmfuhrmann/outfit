package github.io.ddmfuhrmann.outfit.finance.application.dto;

import github.io.ddmfuhrmann.outfit.finance.domain.model.Receivable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableResponse(
        Long id,
        Long saleId,
        Long customerId,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal balance,
        String status) {

    public static ReceivableResponse from(Receivable receivable) {
        return new ReceivableResponse(
                receivable.getId(),
                receivable.getSaleId(),
                receivable.getCustomerId(),
                receivable.getDueDate(),
                receivable.getAmount(),
                receivable.getBalance(),
                receivable.getStatus().name());
    }
}
