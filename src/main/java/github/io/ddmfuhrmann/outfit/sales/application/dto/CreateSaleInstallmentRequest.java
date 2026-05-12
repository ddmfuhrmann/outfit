package github.io.ddmfuhrmann.outfit.sales.application.dto;

import java.math.BigDecimal;

// Stub — fields will be finalised in phase 4b-2 when CreateSaleUseCase is implemented
public record CreateSaleInstallmentRequest(String paymentMethod, BigDecimal amount) {}
