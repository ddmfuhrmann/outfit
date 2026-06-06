CREATE TABLE receivable (
    id                         BIGINT         NOT NULL,
    sale_id                    BIGINT         NOT NULL,
    customer_id                BIGINT         NOT NULL,
    due_date                   DATE           NOT NULL,
    amount                     NUMERIC(15,2)  NOT NULL,
    balance                    NUMERIC(15,2)  NOT NULL,
    sale_total_deferred_amount NUMERIC(15,2)  NOT NULL,
    status                     VARCHAR(20)    NOT NULL,
    version                    BIGINT         NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ    NOT NULL,
    updated_at                 TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_receivable PRIMARY KEY (id)
);

CREATE TABLE receivable_payment (
    id             BIGINT         NOT NULL,
    receivable_id  BIGINT         NOT NULL,
    paid_at        TIMESTAMPTZ    NOT NULL,
    amount         NUMERIC(15,2)  NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_receivable_payment PRIMARY KEY (id),
    CONSTRAINT fk_receivable_payment_receivable FOREIGN KEY (receivable_id) REFERENCES receivable(id)
);

CREATE INDEX idx_receivable_sale ON receivable(sale_id);
CREATE INDEX idx_receivable_customer ON receivable(customer_id);
CREATE INDEX idx_receivable_payment_receivable ON receivable_payment(receivable_id);
