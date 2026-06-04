package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleOrigin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateSaleRequest(
        @NotNull Long customerId,
        @NotNull SaleOrigin origin,
        Long consignmentId,
        @NotNull LocalDate issueDate,
        Long storeCreditNoteId,
        BigDecimal storeCreditDiscount,
        String notes,
        @NotEmpty @Valid List<CreateSaleItemRequest> items,
        @NotEmpty @Valid List<CreateSaleInstallmentRequest> installments,
        @NotEmpty @Valid List<CreateSaleSellerRequest> sellers) {}
