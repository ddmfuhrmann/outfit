package github.io.ddmfuhrmann.outfit.catalog.application.dto;

import github.io.ddmfuhrmann.outfit.catalog.domain.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ProductResponse(
        Long id,
        String description,
        BigDecimal price,
        BigDecimal cost,
        LocalDate purchaseDate,
        Long colorId,
        String colorName,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        boolean active,
        List<ProductSkuResponse> skus
) {
    public static ProductResponse from(Product product, String colorName, String brandName,
                                       String categoryName, Map<Long, String> sizeNames) {
        var skus = product.getSkus().stream()
                .map(sku -> ProductSkuResponse.from(sku, sizeNames.getOrDefault(sku.getSizeId(), "")))
                .toList();
        return new ProductResponse(
                product.getId(),
                product.getDescription(),
                product.getPrice(),
                product.getCost(),
                product.getPurchaseDate(),
                product.getColorId(),
                colorName,
                product.getBrandId(),
                brandName,
                product.getCategoryId(),
                categoryName,
                product.isActive(),
                skus
        );
    }
}
