package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.application.dto.ConsignmentResponse;
import github.io.ddmfuhrmann.outfit.sales.application.dto.IssueConsignmentRequest;
import github.io.ddmfuhrmann.outfit.sales.domain.model.Consignment;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.ConsignmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class IssueConsignmentUseCase {

    private final ConsignmentRepository repository;

    public IssueConsignmentUseCase(ConsignmentRepository repository) {
        this.repository = repository;
    }

    public ConsignmentResponse execute(IssueConsignmentRequest request) {
        var itemInputs = buildItemInputs(request);
        var consignment = Consignment.create(
                request.customerId(),
                request.salespersonIds(),
                request.issueDate(),
                request.notes(),
                itemInputs
        );
        repository.save(consignment);
        log.info("Consignment {} issued for customer {}", consignment.getId(), request.customerId());
        return ConsignmentResponse.from(consignment);
    }

    private java.util.List<Consignment.ConsignmentItemInput> buildItemInputs(IssueConsignmentRequest request) {
        return request.items().stream()
                .map(i -> new Consignment.ConsignmentItemInput(i.skuId(), i.productId(), i.quantity(), i.unitPrice()))
                .toList();
    }
}
