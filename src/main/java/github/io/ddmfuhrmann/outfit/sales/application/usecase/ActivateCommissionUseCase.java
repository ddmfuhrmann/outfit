package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.sales.domain.repository.SellerCommissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class ActivateCommissionUseCase {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SellerCommissionRepository sellerCommissionRepository;

    public ActivateCommissionUseCase(SellerCommissionRepository sellerCommissionRepository) {
        this.sellerCommissionRepository = sellerCommissionRepository;
    }

    @Transactional
    public void execute(Long saleId, BigDecimal receivableAmount, BigDecimal saleTotalDeferredAmount) {
        if (saleTotalDeferredAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        var commissions = sellerCommissionRepository.findBySaleId(saleId);

        for (var commission : commissions) {
            BigDecimal pendingReduction = computeReduction(
                    receivableAmount, commission.getDeferredAmount(),
                    saleTotalDeferredAmount, commission.getCommissionPercent());

            commission.activateProportion(pendingReduction);
            sellerCommissionRepository.save(commission);
        }

        log.info("Activated commissions for sale {} — receivable amount {}", saleId, receivableAmount);
    }

    private BigDecimal computeReduction(BigDecimal receivableAmount, BigDecimal deferredAmount,
                                         BigDecimal saleTotalDeferredAmount, BigDecimal commissionPercent) {
        return receivableAmount
                .multiply(deferredAmount)
                .divide(saleTotalDeferredAmount, 2, RoundingMode.HALF_UP)
                .multiply(commissionPercent)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
