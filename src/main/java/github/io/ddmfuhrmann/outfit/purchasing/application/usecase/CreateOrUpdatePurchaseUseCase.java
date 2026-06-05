package github.io.ddmfuhrmann.outfit.purchasing.application.usecase;

import github.io.ddmfuhrmann.outfit.purchasing.domain.model.Purchase;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;
import github.io.ddmfuhrmann.outfit.purchasing.domain.repository.PurchaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
public class CreateOrUpdatePurchaseUseCase {

    private final PurchaseRepository purchaseRepository;

    public CreateOrUpdatePurchaseUseCase(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Retryable(retryFor = {DataIntegrityViolationException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class}, maxAttempts = 3)
    @Transactional
    public void execute(Long brandId, Long supplierId, LocalDate purchaseDate, String observations,
                        Long productSkuId, int quantity, BigDecimal unitCost) {
        var existing = purchaseRepository.findByBrandIdAndPurchaseDateAndStatus(brandId, purchaseDate, PurchaseStatus.OPEN);

        Purchase purchase;
        if (existing.isPresent()) {
            purchase = existing.get();
            purchase.addLine(productSkuId, quantity, unitCost);
        } else {
            purchase = Purchase.create(brandId, supplierId, purchaseDate, observations);
            purchase.addLine(productSkuId, quantity, unitCost);
        }

        purchaseRepository.save(purchase);
        log.info("Purchase saved: brandId={}, purchaseDate={}, skuId={}", brandId, purchaseDate, productSkuId);
    }
}
