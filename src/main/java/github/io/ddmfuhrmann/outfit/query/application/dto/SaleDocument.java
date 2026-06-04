package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SaleDocument(
        Long saleId,
        Long customerId,
        String customerName,
        SaleCustomerDocument customer,
        String origin,
        Long consignmentId,
        LocalDate issueDate,
        BigDecimal grossAmount,
        BigDecimal storeCreditDiscount,
        BigDecimal netAmount,
        List<SaleSellerDocument> sellers,
        List<SaleItemDocument> items,
        List<SaleInstallmentDocument> installments,
        Instant indexedAt) {}
