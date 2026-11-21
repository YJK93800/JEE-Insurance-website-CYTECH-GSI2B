-- V4 : triggers (règles de cohérence), après les données pour ne pas gêner l'insertion

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
