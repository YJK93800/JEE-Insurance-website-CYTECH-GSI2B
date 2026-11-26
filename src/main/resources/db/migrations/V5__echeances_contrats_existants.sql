-- V5 : échéancier mensuel des contrats créés avant l'Étape 3 (paiements)
--
-- Depuis l'Étape 3, ContractService crée une échéance par mois à la création et au renouvellement
-- d'un contrat (PaymentSchedule). Les contrats en cours créés avant n'en ont aucune : on les génère ici,
-- avec la même règle que le code Java :
--   échéance n = date_debut + n mois (DATE_ADD ramène le 31 au dernier jour des mois courts, comme plusMonths),
--   tant qu'elle ne dépasse pas date_fin ; montant = prime ; statut EN_ATTENTE.
-- Seuls les contrats EN_ATTENTE, ACTIF ou SUSPENDU sans aucun paiement sont concernés
-- (le contrat de démo de V3 a déjà ses paiements). Les retards sont calculés ensuite par la tâche horaire
-- (ContractExpirationListener -> PaymentService.markOverdue).

INSERT INTO paiement (montant, date_echeance, statut, contrat_id)
WITH RECURSIVE mois (n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM mois WHERE n < 119          -- 10 ans maximum par contrat
)
SELECT c.prime, DATE_ADD(c.date_debut, INTERVAL m.n MONTH), 'EN_ATTENTE', c.id
FROM contrat c
JOIN mois m ON DATE_ADD(c.date_debut, INTERVAL m.n MONTH) <= c.date_fin
WHERE c.statut IN ('EN_ATTENTE', 'ACTIF', 'SUSPENDU')
  AND c.prime > 0                                  -- chk_paiement_montant : montant > 0
  AND NOT EXISTS (SELECT 1 FROM paiement p WHERE p.contrat_id = c.id);
