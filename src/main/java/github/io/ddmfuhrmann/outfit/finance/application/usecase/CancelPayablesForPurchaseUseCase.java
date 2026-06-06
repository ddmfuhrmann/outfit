package github.io.ddmfuhrmann.outfit.finance.application.usecase;

import github.io.ddmfuhrmann.outfit.finance.domain.model.PayableStatus;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.PayableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CancelPayablesForPurchaseUseCase {

    private final PayableRepository payableRepository;

    public CancelPayablesForPurchaseUseCase(PayableRepository payableRepository) {
        this.payableRepository = payableRepository;
    }

    @Transactional
    public void execute(Long purchaseId) {
        var payables = payableRepository.findByPurchaseId(purchaseId);

        payables.stream()
                .filter(p -> p.getStatus() != PayableStatus.PAID && p.getStatus() != PayableStatus.CANCELLED)
                .forEach(p -> {
                    p.cancel();
                    payableRepository.save(p);
                });

        log.info("Cancelled payables for purchase {}", purchaseId);
    }
}
