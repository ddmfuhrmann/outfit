package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.ConsignmentResponse;
import github.io.ddmfuhrmann.outfit.sales.application.dto.ReturnItemRequest;
import github.io.ddmfuhrmann.outfit.sales.application.dto.ReturnItemsRequest;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.ConsignmentRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ReturnConsignmentItemsUseCase {

    private final ConsignmentRepository repository;

    public ReturnConsignmentItemsUseCase(ConsignmentRepository repository) {
        this.repository = repository;
    }

    public ConsignmentResponse execute(Long consignmentId, ReturnItemsRequest request) {
        var consignment = repository.findById(consignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Consignment not found: " + consignmentId));

        Map<Long, Integer> quantitiesBySkuId = request.items().stream()
                .collect(Collectors.toMap(ReturnItemRequest::skuId, ReturnItemRequest::quantityReturned));

        consignment.returnItems(quantitiesBySkuId);
        repository.save(consignment);

        log.info("Items returned for consignment {}", consignmentId);
        return ConsignmentResponse.from(consignment);
    }
}
