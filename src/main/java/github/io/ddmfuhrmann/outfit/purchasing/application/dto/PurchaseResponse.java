package github.io.ddmfuhrmann.outfit.purchasing.application.dto;

import github.io.ddmfuhrmann.outfit.purchasing.domain.model.Purchase;

import java.time.LocalDate;
import java.util.List;

public record PurchaseResponse(
        Long id,
        Long brandId,
        Long supplierId,
        LocalDate purchaseDate,
        String observations,
        String status,
        List<PurchaseLineResponse> lines,
        List<PurchasePayableResponse> payables) {

    public static PurchaseResponse from(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getBrandId(),
                purchase.getSupplierId(),
                purchase.getPurchaseDate(),
                purchase.getObservations(),
                purchase.getStatus().name(),
                purchase.getLines().stream().map(PurchaseLineResponse::from).toList(),
                purchase.getPayables().stream().map(PurchasePayableResponse::from).toList());
    }
}
