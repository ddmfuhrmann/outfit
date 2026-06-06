package github.io.ddmfuhrmann.outfit.finance.application.usecase;

import github.io.ddmfuhrmann.outfit.finance.domain.model.Receivable;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.ReceivableRepository;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleConfirmed;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleInstallmentSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class CreateReceivablesFromSaleUseCase {

    private final ReceivableRepository receivableRepository;

    public CreateReceivablesFromSaleUseCase(ReceivableRepository receivableRepository) {
        this.receivableRepository = receivableRepository;
    }

    @Transactional
    public void execute(SaleConfirmed event) {
        BigDecimal saleTotalDeferredAmount = computeTotalDeferredAmount(event);

        if (saleTotalDeferredAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        for (var installment : event.installments()) {
            if (!installment.isDeferred()) continue;
            var receivable = Receivable.create(
                    event.saleId(),
                    event.customerId(),
                    installment.dueDate(),
                    installment.amount(),
                    saleTotalDeferredAmount);
            receivableRepository.save(receivable);
        }

        log.info("Created receivables for sale {}", event.saleId());
    }

    private BigDecimal computeTotalDeferredAmount(SaleConfirmed event) {
        return event.installments().stream()
                .filter(SaleInstallmentSnapshot::isDeferred)
                .map(SaleInstallmentSnapshot::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
