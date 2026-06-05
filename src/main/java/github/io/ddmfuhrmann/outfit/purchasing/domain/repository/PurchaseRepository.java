package github.io.ddmfuhrmann.outfit.purchasing.domain.repository;

import github.io.ddmfuhrmann.outfit.purchasing.domain.model.Purchase;
import github.io.ddmfuhrmann.outfit.purchasing.domain.model.PurchaseStatus;

import java.time.LocalDate;
import java.util.Optional;

public interface PurchaseRepository {

    Optional<Purchase> findByBrandIdAndPurchaseDateAndStatus(Long brandId, LocalDate purchaseDate, PurchaseStatus status);

    Purchase save(Purchase purchase);
}
