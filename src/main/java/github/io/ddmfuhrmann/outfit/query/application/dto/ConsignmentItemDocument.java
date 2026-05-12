package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;

public record ConsignmentItemDocument(
        Long skuId,
        Long productId,
        String productDescription,
        String brandDescription,
        String colorDescription,
        String sizeDescription,
        int quantityIssued,
        int quantityReturned,
        int quantitySold,
        BigDecimal unitPrice) {}
