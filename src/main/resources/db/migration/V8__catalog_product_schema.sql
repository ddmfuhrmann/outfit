CREATE TABLE product (
    id            BIGINT         PRIMARY KEY,
    description   VARCHAR(300)   NOT NULL,
    price         NUMERIC(15,2)  NOT NULL,
    cost          NUMERIC(15,2)  NOT NULL,
    purchase_date DATE,
    color_id      BIGINT         REFERENCES color(id),
    brand_id      BIGINT         NOT NULL REFERENCES brand(id),
    category_id   BIGINT         NOT NULL REFERENCES category(id),
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE product_sku (
    id         BIGINT       PRIMARY KEY,
    product_id BIGINT       NOT NULL REFERENCES product(id),
    barcode    VARCHAR(50)  NOT NULL UNIQUE,
    size_id    BIGINT       NOT NULL REFERENCES size(id),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
