-- V3 : données de référence et comptes de démo (mot de passe : Assurance2026!)

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
