-- V1 : création des tables (extrait de db/script.sql)

-- ------------------------------------------------------------
-- USERS & rôles (client / agent / admin )
-- ------------------------------------------------------------
CREATE TABLE role (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom  VARCHAR(30) NOT NULL UNIQUE,
    CONSTRAINT chk_role_nom CHECK (nom IN ('CLIENT', 'AGENT', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE utilisateur (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(80)  NOT NULL,
    prenom          VARCHAR(80)  NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(255) NOT NULL,           -- hash BCrypt, jamais en clair
    telephone       VARCHAR(20),
    adresse         VARCHAR(255),
    date_naissance  DATE,
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,   -- FALSE = compte désactivé
    date_creation   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    role_id         BIGINT       NOT NULL,
    CONSTRAINT chk_utilisateur_email CHECK (email LIKE '%_@_%._%'),
    CONSTRAINT fk_utilisateur_role FOREIGN KEY (role_id) REFERENCES role(id),
    INDEX idx_utilisateur_nom (nom, prenom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- TYPES ASSURANCE & GARANTIE
-- ------------------------------------------------------------
CREATE TABLE type_assurance (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100)  NOT NULL UNIQUE,
    description TEXT,
    prix_base   DECIMAL(10,2) NOT NULL,
    actif       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_type_prix CHECK (prix_base >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE garantie (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    actif       BOOLEAN      NOT NULL DEFAULT TRUE   
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- GARANTIE POUR CHAQUE TYPE D'ASSU
CREATE TABLE type_assurance_garantie (
    type_assurance_id BIGINT NOT NULL,
    garantie_id       BIGINT NOT NULL,
    PRIMARY KEY (type_assurance_id, garantie_id),
    CONSTRAINT fk_tag_type     FOREIGN KEY (type_assurance_id) REFERENCES type_assurance(id) ON DELETE CASCADE,
    CONSTRAINT fk_tag_garantie FOREIGN KEY (garantie_id)       REFERENCES garantie(id)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- CONTRATS
-- ------------------------------------------------------------
CREATE TABLE contrat (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero               VARCHAR(30)   NOT NULL UNIQUE,
    date_debut           DATE          NOT NULL,
    date_fin             DATE          NOT NULL,
    prime                DECIMAL(10,2) NOT NULL,
    statut               VARCHAR(20)   NOT NULL DEFAULT 'EN_ATTENTE',
    client_id            BIGINT        NOT NULL,
    type_assurance_id    BIGINT        NOT NULL,
    agent_id             BIGINT,                      -- agent qui gère le contrat
    contrat_precedent_id BIGINT,                      -- renouvellement : contrat d'origine
    date_resiliation     DATE,                        -- renseignée si statut = RESILIE
    motif_resiliation    VARCHAR(255),
    date_creation        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_contrat_statut      CHECK (statut IN ('EN_ATTENTE', 'ACTIF', 'SUSPENDU', 'EXPIRE', 'RESILIE')),
    CONSTRAINT chk_contrat_dates       CHECK (date_fin > date_debut),
    CONSTRAINT chk_contrat_prime       CHECK (prime >= 0),
    CONSTRAINT chk_contrat_resiliation CHECK (statut <> 'RESILIE' OR date_resiliation IS NOT NULL),
    CONSTRAINT chk_contrat_date_resil  CHECK (date_resiliation IS NULL OR date_resiliation >= date_debut),
    CONSTRAINT fk_contrat_client     FOREIGN KEY (client_id)            REFERENCES utilisateur(id),
    CONSTRAINT fk_contrat_type       FOREIGN KEY (type_assurance_id)    REFERENCES type_assurance(id),
    CONSTRAINT fk_contrat_agent      FOREIGN KEY (agent_id)             REFERENCES utilisateur(id),
    CONSTRAINT fk_contrat_precedent  FOREIGN KEY (contrat_precedent_id) REFERENCES contrat(id),
    INDEX idx_contrat_statut (statut),
    INDEX idx_contrat_dates  (date_debut, date_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- GARANTIES SOUSCRITES DANS UN CONTRAT
CREATE TABLE contrat_garantie (
    contrat_id  BIGINT NOT NULL,
    garantie_id BIGINT NOT NULL,
    PRIMARY KEY (contrat_id, garantie_id),
    CONSTRAINT fk_cg_contrat  FOREIGN KEY (contrat_id)  REFERENCES contrat(id) ON DELETE CASCADE,
    CONSTRAINT fk_cg_garantie FOREIGN KEY (garantie_id) REFERENCES garantie(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- SINISTRES
-- ------------------------------------------------------------
CREATE TABLE sinistre (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero             VARCHAR(30)   NOT NULL UNIQUE,
    date_sinistre      DATE          NOT NULL,
    description        TEXT          NOT NULL,
    type_sinistre      VARCHAR(30)   NOT NULL,
    montant_estime     DECIMAL(12,2) NOT NULL,
    montant_indemnise  DECIMAL(12,2),
    date_indemnisation DATE,
    statut             VARCHAR(20)   NOT NULL DEFAULT 'DECLARE',
    contrat_id         BIGINT        NOT NULL,
    agent_id           BIGINT,                       -- agent qui traite le dossier
    date_declaration   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_sinistre_statut  CHECK (statut IN ('DECLARE', 'EN_ANALYSE', 'ACCEPTE', 'REFUSE', 'INDEMNISE')),
    CONSTRAINT chk_sinistre_type    CHECK (type_sinistre IN ('VOL', 'INCENDIE', 'ACCIDENT', 'DEGATS_DES_EAUX', 'BRIS', 'MALADIE', 'AUTRE')),
    CONSTRAINT chk_sinistre_montant CHECK (montant_estime >= 0 AND (montant_indemnise IS NULL OR montant_indemnise >= 0)),
    CONSTRAINT chk_sinistre_indemn  CHECK (statut <> 'INDEMNISE' OR (montant_indemnise IS NOT NULL AND date_indemnisation IS NOT NULL)),
    CONSTRAINT fk_sinistre_contrat  FOREIGN KEY (contrat_id) REFERENCES contrat(id),
    CONSTRAINT fk_sinistre_agent    FOREIGN KEY (agent_id)   REFERENCES utilisateur(id),
    INDEX idx_sinistre_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE piece_justificative (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom_fichier VARCHAR(255) NOT NULL,
    chemin      VARCHAR(500) NOT NULL,               
    type_mime   VARCHAR(100),
    taille_octets BIGINT,
    date_ajout  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sinistre_id BIGINT       NOT NULL,
    CONSTRAINT fk_piece_sinistre FOREIGN KEY (sinistre_id) REFERENCES sinistre(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE commentaire_sinistre (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    contenu       TEXT      NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sinistre_id   BIGINT    NOT NULL,
    auteur_id     BIGINT    NOT NULL,
    CONSTRAINT fk_comm_sinistre FOREIGN KEY (sinistre_id) REFERENCES sinistre(id) ON DELETE CASCADE,
    CONSTRAINT fk_comm_auteur   FOREIGN KEY (auteur_id)   REFERENCES utilisateur(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Historique des changements de statut (suivi côté client + traçabilité)
CREATE TABLE historique_sinistre (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sinistre_id     BIGINT      NOT NULL,
    ancien_statut   VARCHAR(20),
    nouveau_statut  VARCHAR(20) NOT NULL,
    date_changement TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modifie_par_id  BIGINT,
    CONSTRAINT chk_hs_ancien  CHECK (ancien_statut IS NULL OR ancien_statut IN ('DECLARE', 'EN_ANALYSE', 'ACCEPTE', 'REFUSE', 'INDEMNISE')),
    CONSTRAINT chk_hs_nouveau CHECK (nouveau_statut IN ('DECLARE', 'EN_ANALYSE', 'ACCEPTE', 'REFUSE', 'INDEMNISE')),
    CONSTRAINT fk_hs_sinistre FOREIGN KEY (sinistre_id)    REFERENCES sinistre(id) ON DELETE CASCADE,
    CONSTRAINT fk_hs_user     FOREIGN KEY (modifie_par_id) REFERENCES utilisateur(id),
    INDEX idx_hs_sinistre (sinistre_id, date_changement)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- PAIEMENTS (échéancier + historique )
-- ------------------------------------------------------------
CREATE TABLE paiement (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    montant           DECIMAL(10,2) NOT NULL,
    date_echeance     DATE          NOT NULL,       
    date_paiement     DATE,                          -- NULL tant que non payé
    mode_paiement     VARCHAR(20),
    statut            VARCHAR(20)   NOT NULL DEFAULT 'EN_ATTENTE',
    contrat_id        BIGINT        NOT NULL,
    enregistre_par_id BIGINT,
    CONSTRAINT chk_paiement_statut   CHECK (statut IN ('EN_ATTENTE', 'EFFECTUE', 'EN_RETARD', 'ANNULE')),
    CONSTRAINT chk_paiement_mode     CHECK (mode_paiement IS NULL OR mode_paiement IN ('CARTE', 'VIREMENT', 'PRELEVEMENT', 'CHEQUE', 'ESPECES')),
    CONSTRAINT chk_paiement_montant  CHECK (montant > 0),
    CONSTRAINT chk_paiement_effectue CHECK (statut <> 'EFFECTUE' OR (date_paiement IS NOT NULL AND mode_paiement IS NOT NULL)),
    CONSTRAINT fk_paiement_contrat   FOREIGN KEY (contrat_id)        REFERENCES contrat(id),
    CONSTRAINT fk_paiement_agent     FOREIGN KEY (enregistre_par_id) REFERENCES utilisateur(id),
    INDEX idx_paiement_statut (statut, date_echeance)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- ASSISTANT INTELLIGENT
-- ------------------------------------------------------------
CREATE TABLE message_ia (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id    BIGINT      NOT NULL,
    role_message VARCHAR(10) NOT NULL,               -- USER ou ASSISTANT
    contenu      TEXT        NOT NULL,
    date_message TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_msg_role  CHECK (role_message IN ('USER', 'ASSISTANT')),
    CONSTRAINT fk_msg_client FOREIGN KEY (client_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    INDEX idx_message_client (client_id, date_message)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

