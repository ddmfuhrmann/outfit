CREATE TABLE stock_balance (
    product_sku_id  BIGINT      PRIMARY KEY REFERENCES product_sku(id),
    current_balance INT         NOT NULL DEFAULT 0,
    version         BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_entry (
    id              BIGINT       PRIMARY KEY,
    product_sku_id  BIGINT       NOT NULL REFERENCES product_sku(id),
    product_id      BIGINT       NOT NULL REFERENCES product(id),
    quantity        INT          NOT NULL,
    running_balance INT          NOT NULL,
    source          VARCHAR(50)  NOT NULL,
    source_key      BIGINT,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_entry_sku_occurred
    ON stock_entry(product_sku_id, occurred_at DESC);
