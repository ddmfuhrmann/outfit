CREATE TABLE store_credit_note (
    id              BIGINT          NOT NULL,
    customer_id     BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    total_amount    NUMERIC(15,2)   NOT NULL,
    consumed_by_sale_id BIGINT,
    notes           VARCHAR(500),
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT pk_store_credit_note PRIMARY KEY (id)
);

CREATE TABLE store_credit_item (
    id                      BIGINT          NOT NULL,
    store_credit_note_id    BIGINT          NOT NULL,
    sku_id                  BIGINT          NOT NULL,
    product_id              BIGINT          NOT NULL,
    quantity                INT             NOT NULL,
    unit_price              NUMERIC(15,2)   NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    CONSTRAINT pk_store_credit_item PRIMARY KEY (id),
    CONSTRAINT fk_store_credit_item_note FOREIGN KEY (store_credit_note_id) REFERENCES store_credit_note(id)
);
