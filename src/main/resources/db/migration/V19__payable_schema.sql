CREATE TABLE payable (
    id                  BIGINT         NOT NULL,
    purchase_id         BIGINT         NOT NULL,
    purchase_payable_id BIGINT         NOT NULL,
    supplier_id         BIGINT         NOT NULL,
    due_date            DATE           NOT NULL,
    amount              NUMERIC(15,2)  NOT NULL,
    balance             NUMERIC(15,2)  NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_payable PRIMARY KEY (id)
);

CREATE TABLE payable_payment (
    id           BIGINT         NOT NULL,
    payable_id   BIGINT         NOT NULL,
    paid_at      TIMESTAMPTZ    NOT NULL,
    amount       NUMERIC(15,2)  NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_payable_payment PRIMARY KEY (id),
    CONSTRAINT fk_payable_payment_payable FOREIGN KEY (payable_id) REFERENCES payable(id)
);

CREATE INDEX idx_payable_purchase ON payable(purchase_id);
CREATE INDEX idx_payable_supplier ON payable(supplier_id);
CREATE INDEX idx_payable_payment_payable ON payable_payment(payable_id);
