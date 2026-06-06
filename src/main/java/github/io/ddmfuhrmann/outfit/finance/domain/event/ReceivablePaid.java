package github.io.ddmfuhrmann.outfit.finance.domain.event;

import java.math.BigDecimal;

public record ReceivablePaid(
        Long receivableId,
        Long saleId,
        BigDecimal receivableAmount,
        BigDecimal saleTotalDeferredAmount) {}
