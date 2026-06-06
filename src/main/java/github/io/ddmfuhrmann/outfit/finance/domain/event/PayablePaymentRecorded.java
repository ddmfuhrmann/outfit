package github.io.ddmfuhrmann.outfit.finance.domain.event;

import java.math.BigDecimal;

public record PayablePaymentRecorded(Long payableId, BigDecimal newBalance, String newStatus) {}
