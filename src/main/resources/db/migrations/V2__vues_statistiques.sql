-- V2 : vues de statistiques

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
