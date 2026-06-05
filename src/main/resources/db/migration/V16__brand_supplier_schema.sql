CREATE TABLE brand_supplier (
    brand_id    BIGINT NOT NULL REFERENCES brand(id),
    supplier_id BIGINT NOT NULL,
    PRIMARY KEY (brand_id, supplier_id)
);

CREATE INDEX idx_brand_supplier_supplier ON brand_supplier(supplier_id);
