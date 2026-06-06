package github.io.ddmfuhrmann.outfit.finance.domain.event;

import java.math.BigDecimal;

public record ReceivablePaymentRecorded(Long receivableId, BigDecimal newBalance, String newStatus) {}
