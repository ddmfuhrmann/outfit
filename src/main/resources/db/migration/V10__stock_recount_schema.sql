CREATE TABLE stock_recount (
    id          BIGINT       PRIMARY KEY,
    date        DATE         NOT NULL,
    notes       TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    closed_at   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_recount_item (
    id                BIGINT      PRIMARY KEY,
    stock_recount_id  BIGINT      NOT NULL REFERENCES stock_recount(id),
    product_sku_id    BIGINT      NOT NULL REFERENCES product_sku(id),
    counted_qty       INT         NOT NULL,
    system_balance    INT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (stock_recount_id, product_sku_id)
);
