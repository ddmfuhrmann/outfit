CREATE TABLE party (
    id                 BIGINT         PRIMARY KEY,
    person_type        VARCHAR(20)    NOT NULL,
    cnpj               VARCHAR(14),
    cpf                VARCHAR(11),
    legal_name         VARCHAR(200),
    name               VARCHAR(200),
    customer        BOOLEAN        NOT NULL DEFAULT FALSE,
    supplier        BOOLEAN        NOT NULL DEFAULT FALSE,
    salesperson     BOOLEAN        NOT NULL DEFAULT FALSE,
    commission_percent NUMERIC(5,2),
    active             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE party_address (
    id              BIGINT       PRIMARY KEY,
    party_id        BIGINT       NOT NULL REFERENCES party(id),
    street          VARCHAR(300),
    neighborhood    VARCHAR(200),
    zip_code        VARCHAR(8),
    number          VARCHAR(20),
    complement      VARCHAR(100),
    city_ibge_code  INTEGER      REFERENCES city(ibge_city_code),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE party_contact (
    id             BIGINT       PRIMARY KEY,
    party_id       BIGINT       NOT NULL REFERENCES party(id),
    classification VARCHAR(30)  NOT NULL,
    description    VARCHAR(200) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
