CREATE TABLE city (
    id              BIGSERIAL PRIMARY KEY,
    ibge_city_code  INTEGER     NOT NULL UNIQUE,
    ibge_state_code INTEGER     NOT NULL,
    city_name       VARCHAR(100) NOT NULL,
    state_name      VARCHAR(100) NOT NULL,
    state_abbr      VARCHAR(2)  NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE company (
    id           BIGSERIAL PRIMARY KEY,
    cnpj         VARCHAR(14) NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    trade_name   VARCHAR(200),
    street       VARCHAR(300),
    phone        VARCHAR(20),
    city_id      BIGINT REFERENCES city(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    login         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
