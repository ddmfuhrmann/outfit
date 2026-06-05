package github.io.ddmfuhrmann.outfit.sales.application.usecase;

import github.io.ddmfuhrmann.outfit.party.application.SalespersonDetails;
import github.io.ddmfuhrmann.outfit.sales.domain.event.SaleInstallmentSnapshot;
import github.io.ddmfuhrmann.outfit.sales.domain.model.Sale;
import github.io.ddmfuhrmann.outfit.sales.domain.model.SaleSeller;
import github.io.ddmfuhrmann.outfit.sales.domain.model.SellerCommission;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.CommissionBonusTierRepository;
import github.io.ddmfuhrmann.outfit.sales.domain.repository.SellerCommissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CreateCommissionsFromSaleUseCase {

    private final SellerCommissionRepository commissionRepository;
    private final CommissionBonusTierRepository bonusTierRepository;

    public CreateCommissionsFromSaleUseCase(SellerCommissionRepository commissionRepository,
                                            CommissionBonusTierRepository bonusTierRepository) {
        this.commissionRepository = commissionRepository;
        this.bonusTierRepository = bonusTierRepository;
    }

    public void execute(Sale sale, List<SalespersonDetails> sellerDetails) {
        var matchingTier = bonusTierRepository.findActiveMatchingTier(sale.getNetAmount());
        BigDecimal bonusPercent = matchingTier.map(t -> t.getBonusPercent()).orElse(BigDecimal.ZERO);

        var installmentSnapshots = sale.getInstallments().stream()
                .map(i -> new SaleInstallmentSnapshot(i.getPaymentModality().name(), i.getDueDate(), i.getAmount()))
                .toList();

        Map<Long, SalespersonDetails> detailsById = sellerDetails.stream()
                .collect(Collectors.toMap(SalespersonDetails::salespersonId, d -> d));

        List<SellerCommission> commissions = new ArrayList<>();
        for (SaleSeller seller : sale.getSellers()) {
            SalespersonDetails details = detailsById.get(seller.getSalespersonId());
            BigDecimal commissionPercent = (details != null && details.commissionPercent() != null)
                    ? details.commissionPercent() : BigDecimal.ZERO;

            var commission = SellerCommission.builder()
                    .saleId(sale.getId())
                    .salespersonId(seller.getSalespersonId())
                    .saleDate(sale.getIssueDate())
                    .commissionPercent(commissionPercent)
                    .netAmount(sale.getNetAmount())
                    .sharePercent(seller.getSharePercent())
                    .installments(installmentSnapshots)
                    .bonusPercent(bonusPercent)
                    .build();
            commissions.add(commission);
        }

        commissionRepository.saveAll(commissions);
        log.info("Created {} commission(s) for sale {}", commissions.size(), sale.getId());
    }
}
