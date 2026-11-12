-- V1 : schéma initial
-- Ne jamais modifier ce fichier une fois poussé et exécuté par le groupe : créer un V2__..., V3__...

-- ===== Rôles =====
CREATE TABLE IF NOT EXISTS role (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    nom  VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_nom UNIQUE (nom),
    CONSTRAINT ck_role_nom CHECK (nom IN ('CLIENT', 'AGENT', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO role (nom) VALUES ('CLIENT'), ('AGENT'), ('ADMIN');

-- ===== Utilisateurs =====
CREATE TABLE IF NOT EXISTS utilisateur (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    prenom          VARCHAR(60)  NOT NULL,
    nom             VARCHAR(60)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    telephone       VARCHAR(20)  NULL,
    date_naissance  DATE         NOT NULL,
    adresse         VARCHAR(255) NULL,
    mot_de_passe    VARCHAR(60)  NOT NULL,
    role_id         BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_utilisateur_email     UNIQUE (email),
    CONSTRAINT uk_utilisateur_telephone UNIQUE (telephone),
    CONSTRAINT fk_utilisateur_role      FOREIGN KEY (role_id) REFERENCES role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
