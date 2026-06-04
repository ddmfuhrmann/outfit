package github.io.ddmfuhrmann.outfit.sales.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Embeddable
public class SaleSeller {

    @Column(name = "salesperson_id", nullable = false)
    private Long salespersonId;

    @Column(name = "share_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePercent;

    protected SaleSeller() {}

    static SaleSeller of(Long salespersonId, BigDecimal sharePercent) {
        if (salespersonId == null) throw new IllegalArgumentException("salespersonId is required");
        if (sharePercent == null) throw new IllegalArgumentException("sharePercent is required");
        var s = new SaleSeller();
        s.salespersonId = salespersonId;
        s.sharePercent = sharePercent;
        return s;
    }
}
