package github.io.ddmfuhrmann.outfit.finance.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableCreated(
        Long receivableId,
        Long saleId,
        Long customerId,
        LocalDate dueDate,
        BigDecimal amount) {}
