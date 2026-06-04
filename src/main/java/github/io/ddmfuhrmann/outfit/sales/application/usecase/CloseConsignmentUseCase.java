package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CloseConsignmentRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateSaleInstallmentRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateSaleItemRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateSaleRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.CreateSaleSellerRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.SaleResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.model.Consignment;
import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleOrigin;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.ConsignmentRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CloseConsignmentUseCase {

    private final ConsignmentRepository repository;
    private final CreateSaleUseCase createSale;
    private final Clock clock;

    public CloseConsignmentUseCase(ConsignmentRepository repository,
                                   CreateSaleUseCase createSale,
                                   Clock clock) {
        this.repository = repository;
        this.createSale = createSale;
        this.clock = clock;
    }

    public SaleResponse execute(Long consignmentId, CloseConsignmentRequest request) {
        var consignment = repository.findById(consignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consignment not found: " + consignmentId));

        consignment.close(Instant.now(clock));
        repository.save(consignment);
        log.info("Consignment {} closed", consignmentId);

        var soldItems = buildSoldItemRequests(consignment);
        var sellerRequests = buildSellerRequests(request);
        var saleRequest = buildSaleRequest(consignment, soldItems, sellerRequests, request);

        return createSale.execute(saleRequest);
    }

    private List<CreateSaleItemRequest> buildSoldItemRequests(
            Consignment consignment) {
        return consignment.getItems().stream()
                .filter(i -> i.getQuantitySold() > 0)
                .map(i -> new CreateSaleItemRequest(i.getSkuId(), i.getProductId(),
                        i.getQuantitySold(), i.getUnitPrice()))
                .toList();
    }

    private List<CreateSaleSellerRequest> buildSellerRequests(CloseConsignmentRequest request) {
        int count = request.sellerIds().size();
        BigDecimal baseShare = new BigDecimal("100").divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.DOWN);
        BigDecimal remainder = new BigDecimal("100").subtract(baseShare.multiply(BigDecimal.valueOf(count)));
        return request.sellerIds().stream()
                .map(id -> {
                    BigDecimal share = id.equals(request.sellerIds().getFirst()) ? baseShare.add(remainder) : baseShare;
                    return new CreateSaleSellerRequest(id, share);
                })
                .toList();
    }

    private CreateSaleRequest buildSaleRequest(
            Consignment consignment,
            List<CreateSaleItemRequest> soldItems,
            List<CreateSaleSellerRequest> sellerRequests,
            CloseConsignmentRequest request) {
        return new CreateSaleRequest(
                consignment.getCustomerId(),
                SaleOrigin.CONSIGNMENT,
                consignment.getId(),
                consignment.getIssueDate(),
                null,
                BigDecimal.ZERO,
                consignment.getNotes(),
                soldItems,
                request.installments(),
                sellerRequests);
    }
}
