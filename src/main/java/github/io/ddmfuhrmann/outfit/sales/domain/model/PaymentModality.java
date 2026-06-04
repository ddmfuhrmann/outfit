package github.io.ddmfuhrmann.outfit.sales.domain.model;

public enum PaymentModality {
    CASH,
    DEBIT_CARD,
    CREDIT_CARD,
    PIX,
    BANK_TRANSFER,
    CHECK,
    INSTALLMENT,
    STORE_ACCOUNT;

    public boolean isDeferred() {
        return this == INSTALLMENT || this == STORE_ACCOUNT;
    }
}
