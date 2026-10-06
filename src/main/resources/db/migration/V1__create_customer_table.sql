CREATE TABLE customer (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    full_name   VARCHAR(100)   NOT NULL,
    email       VARCHAR(150)   NOT NULL,
    phone_no    VARCHAR(20)    NOT NULL,
    account_no  VARCHAR(12)    NOT NULL,
    balance     NUMERIC(19,2)  NOT NULL CONSTRAINT df_customer_balance DEFAULT 0,
    currency    VARCHAR(3)     NOT NULL CONSTRAINT df_customer_currency DEFAULT 'MYR',
    status      VARCHAR(20)    NOT NULL CONSTRAINT df_customer_status DEFAULT 'ACTIVE',
    version     BIGINT         NOT NULL CONSTRAINT df_customer_version DEFAULT 0,
    created_at  DATETIME2(6)   NOT NULL,
    updated_at  DATETIME2(6)   NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (id),
    CONSTRAINT uk_customer_email UNIQUE (email),
    CONSTRAINT uk_customer_account_no UNIQUE (account_no),
    CONSTRAINT ck_customer_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'))
);

CREATE INDEX ix_customer_status ON customer (status);
