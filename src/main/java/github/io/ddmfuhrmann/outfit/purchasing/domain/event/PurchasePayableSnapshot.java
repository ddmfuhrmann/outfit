package github.io.ddmfuhrmann.outfit.purchasing.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchasePayableSnapshot(LocalDate dueDate, BigDecimal amount) {}
