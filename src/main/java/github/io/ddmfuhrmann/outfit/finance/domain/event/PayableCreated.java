package github.io.ddmfuhrmann.outfit.finance.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayableCreated(
        Long payableId,
        Long purchaseId,
        Long supplierId,
        LocalDate dueDate,
        BigDecimal amount) {}
