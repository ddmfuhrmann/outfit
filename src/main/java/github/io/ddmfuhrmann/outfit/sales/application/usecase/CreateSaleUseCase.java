package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.party.application.GetSalespersonDetailsService;
import github.io.ddmfuhrmann.outfit.party.application.SalespersonDetails;
import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateSaleRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.SaleResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.model.Sale;
import github.io.ddmfuhrmann.outfit.sales.domain.model.Sale.SaleInput;
import github.io.ddmfuhrmann.outfit.sales.domain.model.StoreCreditNoteStatus;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.SaleRepository;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.StoreCreditNoteRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CreateSaleUseCase {

    private final SaleRepository saleRepository;
    private final StoreCreditNoteRepository storeCreditNoteRepository;
    private final GetSalespersonDetailsService getSalespersonDetails;
    private final CreateCommissionsFromSaleUseCase createCommissionsFromSale;

    public CreateSaleUseCase(SaleRepository saleRepository,
                             StoreCreditNoteRepository storeCreditNoteRepository,
                             GetSalespersonDetailsService getSalespersonDetails,
                             CreateCommissionsFromSaleUseCase createCommissionsFromSale) {
        this.saleRepository = saleRepository;
        this.storeCreditNoteRepository = storeCreditNoteRepository;
        this.getSalespersonDetails = getSalespersonDetails;
        this.createCommissionsFromSale = createCommissionsFromSale;
    }

    public SaleResponse execute(CreateSaleRequest request) {
        var grossAmount = computeGrossAmount(request);
        var storeCreditDiscount = resolveStoreCreditDiscount(request, grossAmount);
        var netAmount = grossAmount.subtract(storeCreditDiscount);
        var sellerDetails = fetchSellerDetails(request);
        var sellers = toSellerInputs(sellerDetails, request);

        var input = new SaleInput(
                request.customerId(),
                request.origin(),
                request.consignmentId(),
                request.issueDate(),
                grossAmount,
                storeCreditDiscount,
                netAmount,
                request.storeCreditNoteId(),
                request.notes(),
                buildItemInputs(request),
                buildInstallmentInputs(request),
                sellers);

        var sale = Sale.create(input);
        saleRepository.save(sale);

        consumeStoreCreditNote(request, sale);
        createCommissionsFromSale.execute(sale, sellerDetails);

        log.info("Sale {} created for customer {} (origin={})", sale.getId(), request.customerId(), request.origin());
        return SaleResponse.from(sale);
    }

    private BigDecimal computeGrossAmount(CreateSaleRequest request) {
        return request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveStoreCreditDiscount(CreateSaleRequest request, BigDecimal grossAmount) {
        if (request.storeCreditNoteId() == null) {
            return request.storeCreditDiscount() != null ? request.storeCreditDiscount() : BigDecimal.ZERO;
        }

        var note = storeCreditNoteRepository.findById(request.storeCreditNoteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "StoreCreditNote not found: " + request.storeCreditNoteId()));

        if (note.getStatus() != StoreCreditNoteStatus.OPEN) {
            throw new IllegalStateException("store credit note is already consumed");
        }
        if (!note.getCustomerId().equals(request.customerId())) {
            throw new IllegalArgumentException("store credit note does not belong to this customer");
        }

        return note.getTotalAmount().min(grossAmount);
    }

    private void consumeStoreCreditNote(CreateSaleRequest request, Sale sale) {
        if (request.storeCreditNoteId() == null) return;

        var note = storeCreditNoteRepository.findById(request.storeCreditNoteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "StoreCreditNote not found: " + request.storeCreditNoteId()));
        note.consume(sale.getId());
        storeCreditNoteRepository.save(note);
    }

    private List<SalespersonDetails> fetchSellerDetails(CreateSaleRequest request) {
        return request.sellers().stream()
                .map(s -> getSalespersonDetails.execute(s.salespersonId()))
                .toList();
    }

    private List<SaleInput.SellerInput> toSellerInputs(List<SalespersonDetails> sellerDetails,
                                                        CreateSaleRequest request) {
        return request.sellers().stream()
                .map(s -> new SaleInput.SellerInput(s.salespersonId(), s.sharePercent()))
                .toList();
    }

    private List<SaleInput.ItemInput> buildItemInputs(CreateSaleRequest request) {
        return request.items().stream()
                .map(i -> new SaleInput.ItemInput(i.skuId(), i.productId(), i.quantity(), i.unitPrice()))
                .toList();
    }

    private List<SaleInput.InstallmentInput> buildInstallmentInputs(CreateSaleRequest request) {
        return request.installments().stream()
                .map(i -> new SaleInput.InstallmentInput(i.paymentModality(), i.dueDate(), i.amount()))
                .toList();
    }
}
