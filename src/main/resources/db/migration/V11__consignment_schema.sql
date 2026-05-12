CREATE TABLE consignment (
    id                BIGINT          NOT NULL,
    customer_id       BIGINT          NOT NULL,
    status            VARCHAR(20)     NOT NULL,
    issue_date        DATE            NOT NULL,
    closed_at         TIMESTAMPTZ,
    notes             VARCHAR(500),
    created_at        TIMESTAMPTZ     NOT NULL,
    updated_at        TIMESTAMPTZ     NOT NULL,
    version           BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_consignment PRIMARY KEY (id)
);

CREATE TABLE consignment_item (
    id                  BIGINT          NOT NULL,
    consignment_id      BIGINT          NOT NULL,
    sku_id              BIGINT          NOT NULL,
    product_id          BIGINT          NOT NULL,
    unit_price          NUMERIC(15,2)   NOT NULL,
    quantity_issued     INT             NOT NULL,
    quantity_returned   INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    CONSTRAINT pk_consignment_item PRIMARY KEY (id),
    CONSTRAINT fk_consignment_item_consignment FOREIGN KEY (consignment_id) REFERENCES consignment(id)
);

CREATE TABLE consignment_salesperson (
    consignment_id    BIGINT  NOT NULL,
    salesperson_id    BIGINT  NOT NULL,
    CONSTRAINT fk_consignment_salesperson FOREIGN KEY (consignment_id) REFERENCES consignment(id)
);
