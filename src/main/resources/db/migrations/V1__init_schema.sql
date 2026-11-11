CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(60)  NOT NULL,
    name          VARCHAR(60)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    phone_number  VARCHAR(20)  NULL,
    birth_date    DATE         NOT NULL,
    address       VARCHAR(255) NULL,
    password      VARCHAR(60)  NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'CLIENT',
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone_number),
    CONSTRAINT ck_users_role  CHECK (role IN ('CLIENT', 'AGENT', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
