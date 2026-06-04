package github.io.ddmfuhrmann.outfit.sales.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SaleConfirmed(
        Long saleId,
        Long customerId,
        String origin,
        Long consignmentId,
        LocalDate issueDate,
        BigDecimal grossAmount,
        BigDecimal storeCreditDiscount,
        BigDecimal netAmount,
        String notes,
        List<SaleItemSnapshot> items,
        List<SaleInstallmentSnapshot> installments,
        List<SaleSellerSnapshot> sellers) {}
