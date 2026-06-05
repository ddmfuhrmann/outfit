CREATE TABLE purchase (
  id              BIGINT PRIMARY KEY,
  version         BIGINT NOT NULL DEFAULT 0,
  brand_id        BIGINT NOT NULL,
  supplier_id     BIGINT,
  purchase_date   DATE NOT NULL,
  observations    TEXT,
  status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL
);

CREATE TABLE purchase_line (
  id               BIGINT PRIMARY KEY,
  purchase_id      BIGINT NOT NULL REFERENCES purchase(id),
  product_sku_id   BIGINT NOT NULL,
  quantity         INT NOT NULL,
  unit_cost        NUMERIC(15,2) NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL,
  updated_at       TIMESTAMPTZ NOT NULL
);

CREATE TABLE purchase_payable (
  id           BIGINT PRIMARY KEY,
  purchase_id  BIGINT NOT NULL REFERENCES purchase(id),
  due_date     DATE NOT NULL,
  amount       NUMERIC(15,2) NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL,
  updated_at   TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_purchase_open_brand_date ON purchase(brand_id, purchase_date) WHERE status = 'OPEN';
CREATE INDEX idx_purchase_brand_date_status ON purchase(brand_id, purchase_date, status);
CREATE INDEX idx_purchase_line_purchase ON purchase_line(purchase_id);
CREATE INDEX idx_purchase_payable_purchase ON purchase_payable(purchase_id);
