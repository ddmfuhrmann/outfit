CREATE TABLE commission_bonus_tier (
    id              BIGINT          NOT NULL,
    min_amount      NUMERIC(15,2)   NOT NULL,
    max_amount      NUMERIC(15,2)   NOT NULL,
    bonus_percent   NUMERIC(5,2)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL,
    CONSTRAINT pk_commission_bonus_tier PRIMARY KEY (id)
);

CREATE TABLE seller_commission (
    id                  BIGINT          NOT NULL,
    sale_id             BIGINT          NOT NULL,
    salesperson_id      BIGINT          NOT NULL,
    sale_date           DATE            NOT NULL,
    commission_percent  NUMERIC(5,2)    NOT NULL,
    immediate_amount    NUMERIC(15,2)   NOT NULL,
    deferred_amount     NUMERIC(15,2)   NOT NULL,
    commission_base     NUMERIC(15,2)   NOT NULL,
    earned_amount       NUMERIC(15,2)   NOT NULL,
    bonus_amount        NUMERIC(15,2)   NOT NULL,
    pending_amount      NUMERIC(15,2)   NOT NULL,
    net_amount          NUMERIC(15,2)   NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL,
    updated_at          TIMESTAMPTZ     NOT NULL,
    CONSTRAINT pk_seller_commission PRIMARY KEY (id)
);
