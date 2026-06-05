package github.io.ddmfuhrmann.outfit.purchasing.application.dto;

import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchasePayable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchasePayableResponse(Long id, LocalDate dueDate, BigDecimal amount) {

    public static PurchasePayableResponse from(PurchasePayable payable) {
        return new PurchasePayableResponse(payable.getId(), payable.getDueDate(), payable.getAmount());
    }
}
