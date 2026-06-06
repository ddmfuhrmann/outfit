package github.io.ddmfuhrmann.outfit.finance.application.usecase;

import github.io.ddmfuhrmann.outfit.finance.application.dto.PayableResponse;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.PayableRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
public class PayPayableUseCase {

    private final PayableRepository payableRepository;
    private final Clock clock;

    public PayPayableUseCase(PayableRepository payableRepository, Clock clock) {
        this.payableRepository = payableRepository;
        this.clock = clock;
    }

    @Transactional
    public PayableResponse execute(Long id, BigDecimal amount) {
        var payable = payableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payable not found: " + id));

        payable.recordPayment(amount, Instant.now(clock));
        payableRepository.save(payable);

        log.info("Recorded payment of {} on payable {}", amount, id);
        return PayableResponse.from(payable);
    }
}
