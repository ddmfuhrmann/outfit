package github.io.ddmfuhrmann.outfit.finance.application.usecase;

import github.io.ddmfuhrmann.outfit.finance.domain.model.Payable;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.PayableRepository;
import github.io.ddmfuhrmann.outfit.purchasing.domain.event.PurchaseConfirmed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CreatePayablesFromPurchaseUseCase {

    private final PayableRepository payableRepository;

    public CreatePayablesFromPurchaseUseCase(PayableRepository payableRepository) {
        this.payableRepository = payableRepository;
    }

    @Transactional
    public void execute(PurchaseConfirmed event) {
        for (var payableSnapshot : event.payables()) {
            var payable = Payable.create(
                    event.purchaseId(),
                    payableSnapshot.payableId(),
                    event.supplierId(),
                    payableSnapshot.dueDate(),
                    payableSnapshot.amount());
            payableRepository.save(payable);
        }

        log.info("Created payables for purchase {}", event.purchaseId());
    }
}
