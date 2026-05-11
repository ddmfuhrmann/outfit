package github.io.ddmfuhrmann.outfit.inventory.application.dto;

import github.io.ddmfuhrmann.outfit.inventory.domain.model.StockRecountItem;

public record RecountItemResponse(Long skuId, int countedQty, Integer systemBalance, Integer delta) {

    public static RecountItemResponse from(StockRecountItem item) {
        Integer delta = item.getSystemBalance() == null
                ? null
                : item.getCountedQty() - item.getSystemBalance();
        return new RecountItemResponse(item.getProductSkuId(), item.getCountedQty(), item.getSystemBalance(), delta);
    }
}
