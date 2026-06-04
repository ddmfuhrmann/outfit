package github.io.ddmfuhrmann.outfit.sales.application.dto;

import github.io.ddmfuhrmann.outfit.sales.domain.model.Sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SaleResponse(
        Long id,
        Long customerId,
        String origin,
        Long consignmentId,
        LocalDate issueDate,
        BigDecimal grossAmount,
        BigDecimal storeCreditDiscount,
        BigDecimal netAmount,
        Long storeCreditNoteId,
        String notes,
        List<SaleItemResponse> items,
        List<SaleInstallmentResponse> installments,
        List<SaleSellerResponse> sellers,
        Instant createdAt) {

    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getCustomerId(),
                sale.getOrigin().name(),
                sale.getConsignmentId(),
                sale.getIssueDate(),
                sale.getGrossAmount(),
                sale.getStoreCreditDiscount(),
                sale.getNetAmount(),
                sale.getStoreCreditNoteId(),
                sale.getNotes(),
                sale.getItems().stream().map(SaleItemResponse::from).toList(),
                sale.getInstallments().stream().map(SaleInstallmentResponse::from).toList(),
                sale.getSellers().stream().map(SaleSellerResponse::from).toList(),
                sale.getCreatedAt());
    }
}
