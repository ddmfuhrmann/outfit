package github.io.ddmfuhrmann.outfit.finance.application.usecase;

import github.io.ddmfuhrmann.outfit.finance.application.dto.ReceivableResponse;
import github.io.ddmfuhrmann.outfit.finance.domain.repository.ReceivableRepository;
import github.io.ddmfuhrmann.outfit.shared.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
public class PayReceivableUseCase {

    private final ReceivableRepository receivableRepository;
    private final Clock clock;

    public PayReceivableUseCase(ReceivableRepository receivableRepository, Clock clock) {
        this.receivableRepository = receivableRepository;
        this.clock = clock;
    }

    @Transactional
    public ReceivableResponse execute(Long id, BigDecimal amount) {
        var receivable = receivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + id));

        receivable.recordPayment(amount, Instant.now(clock));
        receivableRepository.save(receivable);

        log.info("Recorded payment of {} on receivable {}", amount, id);
        return ReceivableResponse.from(receivable);
    }
}
