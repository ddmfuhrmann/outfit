package github.io.ddmfuhrmann.outfit.purchasing.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchasePayableSnapshot(Long payableId, LocalDate dueDate, BigDecimal amount) {}
