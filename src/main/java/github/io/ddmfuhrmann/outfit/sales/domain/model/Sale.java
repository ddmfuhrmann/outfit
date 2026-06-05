package github.io.ddmfuhrmann.outfit.sales.domain.model;

import github.io.ddmfuhrmann.outfit.sales.domain.event.*;
import github.io.ddmfuhrmann.outfit.shared.domain.model.BaseAggregate;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "sale")
public class Sale extends BaseAggregate<Sale> {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleOrigin origin;

    @Column(name = "consignment_id")
    private Long consignmentId;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "store_credit_discount", nullable = false, precision = 15, scale = 2)
    private BigDecimal storeCreditDiscount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "store_credit_note_id")
    private Long storeCreditNoteId;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "saleId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "saleId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleInstallment> installments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "sale_seller", joinColumns = @JoinColumn(name = "sale_id"))
    private List<SaleSeller> sellers = new ArrayList<>();

    @Version
    private Long version;

    protected Sale() {}

    public static Sale create(SaleInput input) {
        validate(input);

        var sale = new Sale();
        sale.customerId = input.customerId();
        sale.origin = input.origin();
        sale.consignmentId = input.consignmentId();
        sale.issueDate = input.issueDate();
        sale.grossAmount = input.grossAmount();
        sale.storeCreditDiscount = input.storeCreditDiscount() != null
                ? input.storeCreditDiscount() : BigDecimal.ZERO;
        sale.netAmount = input.netAmount();
        sale.storeCreditNoteId = input.storeCreditNoteId();
        sale.notes = input.notes();

        for (var itemInput : input.items()) {
            sale.items.add(SaleItem.create(sale.getId(), itemInput.skuId(), itemInput.productId(),
                    itemInput.quantity(), itemInput.unitPrice()));
        }

        for (var inst : input.installments()) {
            sale.installments.add(SaleInstallment.create(sale.getId(), inst.paymentModality(),
                    inst.dueDate(), inst.amount()));
        }

        for (var seller : input.sellers()) {
            sale.sellers.add(SaleSeller.of(seller.salespersonId(), seller.sharePercent()));
        }

        sale.registerEvent(buildEvent(sale));
        return sale;
    }

    private static void validate(SaleInput input) {
        if (input.customerId() == null) throw new IllegalArgumentException("customerId is required");
        if (input.origin() == null) throw new IllegalArgumentException("origin is required");
        if (input.issueDate() == null) throw new IllegalArgumentException("issueDate is required");
        if (input.grossAmount() == null || input.grossAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("grossAmount must be positive");
        if (input.netAmount() == null || input.netAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("netAmount must be positive");
        if (input.items() == null || input.items().isEmpty())
            throw new IllegalArgumentException("items must not be empty");
        if (input.installments() == null || input.installments().isEmpty())
            throw new IllegalArgumentException("installments must not be empty");
        if (input.sellers() == null || input.sellers().isEmpty())
            throw new IllegalArgumentException("sellers must not be empty");

        validateSellersSharePercent(input.sellers());
        validateInstallmentsSum(input.installments(), input.netAmount());
    }

    private static void validateSellersSharePercent(List<SaleInput.SellerInput> sellers) {
        BigDecimal total = sellers.stream()
                .map(SaleInput.SellerInput::sharePercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100")) != 0)
            throw new IllegalArgumentException("sellers share percents must sum to 100");
    }

    private static void validateInstallmentsSum(List<SaleInput.InstallmentInput> installments, BigDecimal netAmount) {
        BigDecimal total = installments.stream()
                .map(SaleInput.InstallmentInput::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.subtract(netAmount).abs().compareTo(new BigDecimal("0.01")) > 0)
            throw new IllegalArgumentException("installments must sum to netAmount");
    }

    private static SaleConfirmed buildEvent(Sale sale) {
        var itemSnapshots = sale.items.stream()
                .map(i -> new SaleItemSnapshot(i.getSkuId(), i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        var installmentSnapshots = sale.installments.stream()
                .map(i -> new SaleInstallmentSnapshot(i.getPaymentModality().name(), i.getDueDate(), i.getAmount()))
                .toList();
        var sellerSnapshots = sale.sellers.stream()
                .map(s -> new SaleSellerSnapshot(s.getSalespersonId(), s.getSharePercent()))
                .toList();
        return new SaleConfirmed(sale.getId(), sale.customerId, sale.origin.name(), sale.consignmentId,
                sale.issueDate, sale.grossAmount, sale.storeCreditDiscount, sale.netAmount,
                sale.notes, itemSnapshots, installmentSnapshots, sellerSnapshots);
    }

    public record SaleInput(
            Long customerId,
            SaleOrigin origin,
            Long consignmentId,
            LocalDate issueDate,
            BigDecimal grossAmount,
            BigDecimal storeCreditDiscount,
            BigDecimal netAmount,
            Long storeCreditNoteId,
            String notes,
            List<ItemInput> items,
            List<InstallmentInput> installments,
            List<SellerInput> sellers) {

        public record ItemInput(Long skuId, Long productId, int quantity, BigDecimal unitPrice) {}

        public record InstallmentInput(PaymentModality paymentModality, LocalDate dueDate, BigDecimal amount) {}

        public record SellerInput(Long salespersonId, BigDecimal sharePercent) {}
    }
}
