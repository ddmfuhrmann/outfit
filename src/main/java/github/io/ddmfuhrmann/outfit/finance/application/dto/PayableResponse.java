package github.io.ddmfuhrmann.outfit.finance.application.dto;

import github.io.ddmfuhrmann.outfit.finance.domain.model.Payable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayableResponse(
        Long id,
        Long purchaseId,
        Long supplierId,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal balance,
        String status) {

    public static PayableResponse from(Payable payable) {
        return new PayableResponse(
                payable.getId(),
                payable.getPurchaseId(),
                payable.getSupplierId(),
                payable.getDueDate(),
                payable.getAmount(),
                payable.getBalance(),
                payable.getStatus().name());
    }
}
