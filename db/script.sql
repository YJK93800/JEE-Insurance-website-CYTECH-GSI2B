-- ============================================================
-- Plateforme de gestion d'assurance - J2EE ING2 GSI 2026-2027
-- Fichier unique : db/assurance.sql
-- Contenu : tables, vues de statistiques, données de départ + démo, triggers
-- SGBD    : MySQL 8.0.16+
-- Usage   : depuis la racine du projet
--             mysql -u root -p < db/assurance.sql
-- ATTENTION : supprime et recrée la base assurance_db (toutes les données sont perdues).
-- ============================================================

DROP DATABASE IF EXISTS assurance_db;

CREATE DATABASE assurance_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE assurance_db;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- USERS & rôles (client / agent / admin )
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom  VARCHAR(30) NOT NULL UNIQUE,
    CONSTRAINT chk_role_nom CHECK (nom IN ('CLIENT', 'AGENT', 'ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS utilisateur (
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
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- TYPES ASSURANCE & GARANTIE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS type_assurance (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100)  NOT NULL UNIQUE,
    description TEXT,
    prix_base   DECIMAL(10,2) NOT NULL,
    actif       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_type_prix CHECK (prix_base >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS garantie (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    actif       BOOLEAN      NOT NULL DEFAULT TRUE   
) ENGINE=InnoDB;

-- GARANTIE POUR CHAQUE TYPE D'ASSU
CREATE TABLE IF NOT EXISTS type_assurance_garantie (
    type_assurance_id BIGINT NOT NULL,
    garantie_id       BIGINT NOT NULL,
    PRIMARY KEY (type_assurance_id, garantie_id),
    CONSTRAINT fk_tag_type     FOREIGN KEY (type_assurance_id) REFERENCES type_assurance(id) ON DELETE CASCADE,
    CONSTRAINT fk_tag_garantie FOREIGN KEY (garantie_id)       REFERENCES garantie(id)       ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- CONTRATS
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contrat (
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
) ENGINE=InnoDB;

-- GARANTIES SOUSCRITES DANS UN CONTRAT
CREATE TABLE IF NOT EXISTS contrat_garantie (
    contrat_id  BIGINT NOT NULL,
    garantie_id BIGINT NOT NULL,
    PRIMARY KEY (contrat_id, garantie_id),
    CONSTRAINT fk_cg_contrat  FOREIGN KEY (contrat_id)  REFERENCES contrat(id) ON DELETE CASCADE,
    CONSTRAINT fk_cg_garantie FOREIGN KEY (garantie_id) REFERENCES garantie(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
--SINISTRES
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sinistre (
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
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS piece_justificative (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom_fichier VARCHAR(255) NOT NULL,
    chemin      VARCHAR(500) NOT NULL,               
    type_mime   VARCHAR(100),
    taille_octets BIGINT,
    date_ajout  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sinistre_id BIGINT       NOT NULL,
    CONSTRAINT fk_piece_sinistre FOREIGN KEY (sinistre_id) REFERENCES sinistre(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS commentaire_sinistre (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    contenu       TEXT      NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sinistre_id   BIGINT    NOT NULL,
    auteur_id     BIGINT    NOT NULL,
    CONSTRAINT fk_comm_sinistre FOREIGN KEY (sinistre_id) REFERENCES sinistre(id) ON DELETE CASCADE,
    CONSTRAINT fk_comm_auteur   FOREIGN KEY (auteur_id)   REFERENCES utilisateur(id)
) ENGINE=InnoDB;

-- Historique des changements de statut (suivi côté client + traçabilité)
CREATE TABLE IF NOT EXISTS historique_sinistre (
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
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PAIEMENTS (échéancier + historique )
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paiement (
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
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- ASSISTANT INTELLIGENT
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS message_ia (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id    BIGINT      NOT NULL,
    role_message VARCHAR(10) NOT NULL,               -- USER ou ASSISTANT
    contenu      TEXT        NOT NULL,
    date_message TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_msg_role  CHECK (role_message IN ('USER', 'ASSISTANT')),
    CONSTRAINT fk_msg_client FOREIGN KEY (client_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    INDEX idx_message_client (client_id, date_message)
) ENGINE=InnoDB;


-- Contrats par statut (actifs, expirés, résiliés, ...)
CREATE OR REPLACE VIEW v_stats_contrats_par_statut AS
SELECT statut, COUNT(*) AS nb_contrats
FROM contrat
GROUP BY statut;

-- Répartition des clients par type d'assurance (contrats ACTIF uniquement)
CREATE OR REPLACE VIEW v_stats_clients_par_type AS
SELECT ta.id AS type_assurance_id,
       ta.nom AS type_assurance,
       COUNT(DISTINCT c.client_id) AS nb_clients
FROM type_assurance ta
LEFT JOIN contrat c ON c.type_assurance_id = ta.id AND c.statut = 'ACTIF'
GROUP BY ta.id, ta.nom;

-- Évolution mensuelle : nouveaux contrats (par date de début)
CREATE OR REPLACE VIEW v_evolution_contrats_mensuelle AS
SELECT DATE_FORMAT(date_debut, '%Y-%m') AS mois, COUNT(*) AS nb_nouveaux_contrats
FROM contrat
GROUP BY DATE_FORMAT(date_debut, '%Y-%m');

-- Évolution mensuelle : contrats résiliés (par date de résiliation)
CREATE OR REPLACE VIEW v_resiliations_mensuelles AS
SELECT DATE_FORMAT(date_resiliation, '%Y-%m') AS mois, COUNT(*) AS nb_resiliations
FROM contrat
WHERE statut = 'RESILIE' AND date_resiliation IS NOT NULL
GROUP BY DATE_FORMAT(date_resiliation, '%Y-%m');

-- Évolution mensuelle : nouveaux clients
CREATE OR REPLACE VIEW v_evolution_clients_mensuelle AS
SELECT DATE_FORMAT(u.date_creation, '%Y-%m') AS mois, COUNT(*) AS nb_nouveaux_clients
FROM utilisateur u
JOIN role r ON r.id = u.role_id AND r.nom = 'CLIENT'
GROUP BY DATE_FORMAT(u.date_creation, '%Y-%m');

-- Chiffre d'affaires mensuel (paiements effectués, par date de paiement)
CREATE OR REPLACE VIEW v_chiffre_affaires_mensuel AS
SELECT DATE_FORMAT(date_paiement, '%Y-%m') AS mois, SUM(montant) AS chiffre_affaires
FROM paiement
WHERE statut = 'EFFECTUE'
GROUP BY DATE_FORMAT(date_paiement, '%Y-%m');

-- Indemnisations mensuelles (par date d'indemnisation)
CREATE OR REPLACE VIEW v_indemnisations_mensuelles AS
SELECT DATE_FORMAT(date_indemnisation, '%Y-%m') AS mois, SUM(montant_indemnise) AS total_indemnise
FROM sinistre
WHERE statut = 'INDEMNISE'
GROUP BY DATE_FORMAT(date_indemnisation, '%Y-%m');

-- Indicateurs financiers globaux (une seule ligne)
CREATE OR REPLACE VIEW v_stats_financieres AS
SELECT
    (SELECT COALESCE(SUM(prime), 0)             FROM contrat  WHERE statut = 'ACTIF')      AS total_primes_actives,
    (SELECT COALESCE(SUM(montant), 0)           FROM paiement WHERE statut = 'EFFECTUE')   AS chiffre_affaires,
    (SELECT COALESCE(SUM(montant_indemnise), 0) FROM sinistre WHERE statut = 'INDEMNISE')  AS total_indemnisations;

-- ============================================================
-- Données de départ et de démonstration
-- ============================================================

-- ------------------------------------------------------------
-- Données de référence
-- ------------------------------------------------------------
INSERT INTO role (nom) VALUES ('CLIENT'), ('AGENT'), ('ADMIN');

INSERT INTO type_assurance (nom, description, prix_base) VALUES
 ('Assurance automobile',      'Couverture des véhicules terrestres à moteur', 450.00),
 ('Assurance habitation',      'Couverture du logement et des biens',          220.00),
 ('Assurance santé',           'Complémentaire santé',                         600.00),
 ('Assurance voyage',          'Assistance et couverture pendant un voyage',    80.00),
 ('Assurance professionnelle', 'Couverture de l''activité professionnelle',    900.00);

INSERT INTO garantie (nom, description) VALUES
 ('Responsabilité civile', 'Dommages causés à des tiers'),
 ('Vol',                   'Vol ou tentative de vol'),
 ('Incendie',              'Dommages causés par un incendie'),
 ('Accident',              'Dommages accidentels'),
 ('Dégâts des eaux',       'Dommages causés par une fuite ou une inondation'),
 ('Assistance',            'Assistance et dépannage 24h/24');

-- Garanties disponibles par type d'assurance (par noms : ne dépend pas des id)
INSERT INTO type_assurance_garantie (type_assurance_id, garantie_id)
SELECT ta.id, g.id
FROM type_assurance ta
JOIN garantie g ON (ta.nom, g.nom) IN (
    ('Assurance automobile',      'Responsabilité civile'),
    ('Assurance automobile',      'Vol'),
    ('Assurance automobile',      'Incendie'),
    ('Assurance automobile',      'Accident'),
    ('Assurance automobile',      'Assistance'),
    ('Assurance habitation',      'Responsabilité civile'),
    ('Assurance habitation',      'Vol'),
    ('Assurance habitation',      'Incendie'),
    ('Assurance habitation',      'Dégâts des eaux'),
    ('Assurance santé',           'Accident'),
    ('Assurance santé',           'Assistance'),
    ('Assurance voyage',          'Accident'),
    ('Assurance voyage',          'Assistance'),
    ('Assurance professionnelle', 'Responsabilité civile'),
    ('Assurance professionnelle', 'Vol'),
    ('Assurance professionnelle', 'Incendie'),
    ('Assurance professionnelle', 'Dégâts des eaux')
);

-- ------------------------------------------------------------
-- Comptes de démo (mot de passe : Assurance2026!)
-- ------------------------------------------------------------
SET @hash := '$2a$10$MrYC0MJ4KwSIZCcG3oq19.iFaoNH5rEfsvSuzWP2kdzb0YzdmZgXC';

INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, telephone, adresse, role_id)
SELECT 'Admin',  'Super',  'admin@assurance.local',  @hash, NULL,         NULL,                       id FROM role WHERE nom = 'ADMIN';
INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, telephone, adresse, role_id)
SELECT 'Martin', 'Agnès',  'agent@assurance.local',  @hash, '0600000001', NULL,                       id FROM role WHERE nom = 'AGENT';
INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, telephone, adresse, date_naissance, role_id)
SELECT 'Dupont', 'Claire', 'client@assurance.local', @hash, '0600000002', '12 rue des Lilas, 95000 Cergy', '1998-04-12', id FROM role WHERE nom = 'CLIENT';

-- ------------------------------------------------------------
-- Données de démo : un contrat, ses garanties, paiements et sinistres
-- ------------------------------------------------------------
SET @client := (SELECT id FROM utilisateur WHERE email = 'client@assurance.local');
SET @agent  := (SELECT id FROM utilisateur WHERE email = 'agent@assurance.local');
SET @type   := (SELECT id FROM type_assurance WHERE nom = 'Assurance automobile');

INSERT INTO contrat (numero, date_debut, date_fin, prime, statut, client_id, type_assurance_id, agent_id)
VALUES ('CT-2026-0001', '2026-01-01', '2026-12-31', 450.00, 'ACTIF', @client, @type, @agent);
SET @contrat := LAST_INSERT_ID();

INSERT INTO contrat_garantie (contrat_id, garantie_id)
SELECT @contrat, id FROM garantie WHERE nom IN ('Responsabilité civile', 'Vol', 'Accident');

-- Paiements trimestriels de 112,50 € (le dernier est en retard)
INSERT INTO paiement (montant, date_echeance, date_paiement, mode_paiement, statut, contrat_id, enregistre_par_id) VALUES
 (112.50, '2026-01-01', '2026-01-02', 'PRELEVEMENT', 'EFFECTUE', @contrat, @agent),
 (112.50, '2026-04-01', '2026-04-01', 'PRELEVEMENT', 'EFFECTUE', @contrat, @agent),
 (112.50, '2026-07-01', '2026-07-03', 'CARTE',       'EFFECTUE', @contrat, @agent),
 (112.50, '2026-10-01', NULL,         NULL,          'EN_RETARD', @contrat, NULL);

-- Sinistre 1 : en cours d'analyse
INSERT INTO sinistre (numero, date_sinistre, description, type_sinistre, montant_estime, statut, contrat_id, agent_id)
VALUES ('SIN-2026-0001', '2026-06-15', 'Vol du véhicule sur le parking de la gare', 'VOL', 12000.00, 'EN_ANALYSE', @contrat, @agent);
SET @sin1 := LAST_INSERT_ID();

-- Sinistre 2 : déjà indemnisé
INSERT INTO sinistre (numero, date_sinistre, description, type_sinistre, montant_estime, montant_indemnise, date_indemnisation, statut, contrat_id, agent_id)
VALUES ('SIN-2026-0002', '2026-03-10', 'Accrochage au feu rouge, pare-chocs endommagé', 'ACCIDENT', 900.00, 850.00, '2026-04-20', 'INDEMNISE', @contrat, @agent);
SET @sin2 := LAST_INSERT_ID();

INSERT INTO historique_sinistre (sinistre_id, ancien_statut, nouveau_statut, modifie_par_id) VALUES
 (@sin1, NULL,         'DECLARE',    @client),
 (@sin1, 'DECLARE',    'EN_ANALYSE', @agent),
 (@sin2, NULL,         'DECLARE',    @client),
 (@sin2, 'DECLARE',    'ACCEPTE',    @agent),
 (@sin2, 'ACCEPTE',    'INDEMNISE',  @agent);

INSERT INTO commentaire_sinistre (contenu, sinistre_id, auteur_id)
VALUES ('Dépôt de plainte demandé au client avant de poursuivre l''analyse.', @sin1, @agent);

-- ============================================================
-- Triggers (règles de cohérence), après les données pour ne pas gêner l'insertion
-- ============================================================

DELIMITER $$

-- Un contrat appartient à un CLIENT, géré par un AGENT ou un ADMIN ---------
DROP TRIGGER IF EXISTS trg_contrat_bi$$
CREATE TRIGGER trg_contrat_bi BEFORE INSERT ON contrat FOR EACH ROW
BEGIN
    IF (SELECT r.nom FROM utilisateur u JOIN role r ON r.id = u.role_id WHERE u.id = NEW.client_id) <> 'CLIENT' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Le titulaire du contrat doit avoir le role CLIENT';
    END IF;
    IF NEW.agent_id IS NOT NULL
       AND (SELECT r.nom FROM utilisateur u JOIN role r ON r.id = u.role_id WHERE u.id = NEW.agent_id) NOT IN ('AGENT', 'ADMIN') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'L''agent du contrat doit avoir le role AGENT ou ADMIN';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_contrat_bu$$
CREATE TRIGGER trg_contrat_bu BEFORE UPDATE ON contrat FOR EACH ROW
BEGIN
    IF NEW.client_id <> OLD.client_id
       AND (SELECT r.nom FROM utilisateur u JOIN role r ON r.id = u.role_id WHERE u.id = NEW.client_id) <> 'CLIENT' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Le titulaire du contrat doit avoir le role CLIENT';
    END IF;
    IF NEW.agent_id IS NOT NULL AND NOT (NEW.agent_id <=> OLD.agent_id)
       AND (SELECT r.nom FROM utilisateur u JOIN role r ON r.id = u.role_id WHERE u.id = NEW.agent_id) NOT IN ('AGENT', 'ADMIN') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'L''agent du contrat doit avoir le role AGENT ou ADMIN';
    END IF;
END$$

-- Une garantie souscrite doit faire partie de celles du type d'assurance du contrat
DROP TRIGGER IF EXISTS trg_contrat_garantie_bi$$
CREATE TRIGGER trg_contrat_garantie_bi BEFORE INSERT ON contrat_garantie FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM contrat c
                   JOIN type_assurance_garantie tag ON tag.type_assurance_id = c.type_assurance_id
                   WHERE c.id = NEW.contrat_id AND tag.garantie_id = NEW.garantie_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cette garantie n''est pas proposee pour le type d''assurance du contrat';
    END IF;
END$$

-- Un sinistre se déclare sur un contrat ACTIF, pour un fait survenu pendant sa période
DROP TRIGGER IF EXISTS trg_sinistre_bi$$
CREATE TRIGGER trg_sinistre_bi BEFORE INSERT ON sinistre FOR EACH ROW
BEGIN
    DECLARE v_statut VARCHAR(20);
    DECLARE v_debut  DATE;
    DECLARE v_fin    DATE;

    SELECT statut, date_debut, date_fin INTO v_statut, v_debut, v_fin
    FROM contrat WHERE id = NEW.contrat_id;

    IF v_statut <> 'ACTIF' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un sinistre ne peut etre declare que sur un contrat ACTIF';
    END IF;
    IF NEW.date_sinistre < v_debut OR NEW.date_sinistre > v_fin THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La date du sinistre doit etre comprise dans la periode du contrat';
    END IF;
    IF NEW.agent_id IS NOT NULL
       AND (SELECT r.nom FROM utilisateur u JOIN role r ON r.id = u.role_id WHERE u.id = NEW.agent_id) NOT IN ('AGENT', 'ADMIN') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'L''agent du sinistre doit avoir le role AGENT ou ADMIN';
    END IF;
END$$

-- Une fois clos, un sinistre ne revient pas en arrière (REFUSE / INDEMNISE sont définitifs)
DROP TRIGGER IF EXISTS trg_sinistre_bu$$
CREATE TRIGGER trg_sinistre_bu BEFORE UPDATE ON sinistre FOR EACH ROW
BEGIN
    IF OLD.statut IN ('REFUSE', 'INDEMNISE') AND NEW.statut <> OLD.statut THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Le statut d''un sinistre REFUSE ou INDEMNISE ne peut plus changer';
    END IF;
END$$

DELIMITER ;
