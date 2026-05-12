package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.CloseConsignmentRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.SaleResponse;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.ConsignmentRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CloseConsignmentUseCase {

    private final ConsignmentRepository repository;

    public CloseConsignmentUseCase(ConsignmentRepository repository) {
        this.repository = repository;
    }

    public SaleResponse execute(Long consignmentId, CloseConsignmentRequest request) {
        var consignment = repository.findById(consignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consignment not found: " + consignmentId));

        consignment.close();
        repository.save(consignment);

        log.info("Consignment {} closed", consignmentId);

        // TODO: phase 4b-2 — inject and delegate to CreateSaleUseCase
        // grossAmount = sum(item.unitPrice × item.quantitySold) for each sold item
        // origin = CONSIGNMENT, consignmentId = consignment.getId()
        // storeCreditDiscount = BigDecimal.ZERO
        return null;
    }
}
