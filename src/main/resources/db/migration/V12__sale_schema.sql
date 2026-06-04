CREATE TABLE sale (
    id                      BIGINT          NOT NULL,
    customer_id             BIGINT          NOT NULL,
    origin                  VARCHAR(20)     NOT NULL,
    consignment_id          BIGINT,
    issue_date              DATE            NOT NULL,
    gross_amount            NUMERIC(15,2)   NOT NULL,
    store_credit_discount   NUMERIC(15,2)   NOT NULL DEFAULT 0,
    net_amount              NUMERIC(15,2)   NOT NULL,
    store_credit_note_id    BIGINT,
    notes                   VARCHAR(500),
    created_at              TIMESTAMPTZ     NOT NULL,
    updated_at              TIMESTAMPTZ     NOT NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sale PRIMARY KEY (id)
);

CREATE TABLE sale_item (
    id              BIGINT          NOT NULL,
    sale_id         BIGINT          NOT NULL,
    sku_id          BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    unit_price      NUMERIC(15,2)   NOT NULL,
    quantity        INT             NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT pk_sale_item PRIMARY KEY (id),
    CONSTRAINT fk_sale_item_sale FOREIGN KEY (sale_id) REFERENCES sale(id)
);

CREATE TABLE sale_installment (
    id                  BIGINT          NOT NULL,
    sale_id             BIGINT          NOT NULL,
    payment_modality    VARCHAR(30)     NOT NULL,
    due_date            DATE            NOT NULL,
    amount              NUMERIC(15,2)   NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    CONSTRAINT pk_sale_installment PRIMARY KEY (id),
    CONSTRAINT fk_sale_installment_sale FOREIGN KEY (sale_id) REFERENCES sale(id)
);

CREATE TABLE sale_seller (
    sale_id         BIGINT          NOT NULL,
    salesperson_id  BIGINT          NOT NULL,
    share_percent   NUMERIC(5,2)    NOT NULL,
    CONSTRAINT pk_sale_seller PRIMARY KEY (sale_id, salesperson_id),
    CONSTRAINT fk_sale_seller_sale FOREIGN KEY (sale_id) REFERENCES sale(id)
);
