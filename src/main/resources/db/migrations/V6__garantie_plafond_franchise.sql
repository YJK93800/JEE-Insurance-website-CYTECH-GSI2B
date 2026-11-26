-- V6 : plafond et franchise des garanties (Étape 4)
--
-- plafond   : montant maximal remboursé pour un sinistre couvert par la garantie
-- franchise : somme qui reste à la charge du client sur chaque sinistre
-- Les deux sont facultatifs (NULL = pas de plafond / pas de franchise), en euros.

ALTER TABLE garantie
    ADD COLUMN plafond   DECIMAL(12,2) NULL AFTER description,
    ADD COLUMN franchise DECIMAL(12,2) NULL AFTER plafond,
    ADD CONSTRAINT chk_garantie_plafond   CHECK (plafond IS NULL OR plafond >= 0),
    ADD CONSTRAINT chk_garantie_franchise CHECK (franchise IS NULL OR franchise >= 0);

-- Valeurs de démo pour les garanties de V3 (par noms : ne dépend pas des id)
UPDATE garantie SET plafond = 1000000.00, franchise = NULL   WHERE nom = 'Responsabilité civile';
UPDATE garantie SET plafond =   15000.00, franchise = 150.00 WHERE nom = 'Vol';
UPDATE garantie SET plafond =  300000.00, franchise = 300.00 WHERE nom = 'Incendie';
UPDATE garantie SET plafond =   50000.00, franchise = 250.00 WHERE nom = 'Accident';
UPDATE garantie SET plafond =   20000.00, franchise = 150.00 WHERE nom = 'Dégâts des eaux';
UPDATE garantie SET plafond =    NULL,    franchise = NULL   WHERE nom = 'Assistance';
