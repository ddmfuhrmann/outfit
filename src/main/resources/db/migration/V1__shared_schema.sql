CREATE TABLE city (
    ibge_city_code  INTEGER      PRIMARY KEY,
    ibge_state_code INTEGER      NOT NULL,
    city_name       VARCHAR(100) NOT NULL,
    state_name      VARCHAR(100) NOT NULL,
    state_abbr      VARCHAR(2)   NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE company (
    cnpj           VARCHAR(14)  PRIMARY KEY,
    company_name   VARCHAR(200) NOT NULL,
    trade_name     VARCHAR(200),
    street         VARCHAR(300),
    phone          VARCHAR(20),
    city_ibge_code INTEGER      REFERENCES city(ibge_city_code),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE app_user (
    login         VARCHAR(100) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
