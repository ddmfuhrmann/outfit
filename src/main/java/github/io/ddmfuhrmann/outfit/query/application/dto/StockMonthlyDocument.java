package github.io.ddmfuhrmann.outfit.query.application.dto;

public record StockMonthlyDocument(
        Long skuId, Long productId,
        Long brandId, Long categoryId,
        String yearMonth,
        int openingBalance, int totalInbound, int totalOutbound, int closingBalance) {}
