# Projet JEE AssurCY2000 — Checklist

Mise à jour le 28/11/2026. Cochez les cases dans vos commits au fur et à mesure.

## Étape 0 — Authentification et profil (section 2.1)

Codé ; reste à tester dans Tomcat avec les comptes de démo (`client@`, `agent@`, `admin@assurance.local` / `Assurance2026!`).

- [x] Créer un compte (`RegisterServlet`, validation dans `Validation`)
- [x] Se connecter / se déconnecter
- [x] Modifier ses coordonnées : téléphone et adresse (`updateInfos`, `UpdateProfileRequest`)
- [x] Changer son mot de passe (vérification de l'ancien dans `UserService`)
- [x] Nom et prénom verrouillés sur `/profil` ; email et date de naissance modifiables
- [ ] Tester l'inscription : champ vide, moins de 18 ans, email déjà pris, puis inscription valide
- [X] Tester la connexion avec les 3 comptes de démo
- [ ] Tester `/profil` : modifier téléphone et adresse, puis changer le mot de passe (mauvais puis bon mot de passe actuel)
- [ ] Lancer `mvn test` en local

## Utilisateurs côté agent et admin (2.1)

- [x] Agent : consulter les clients (`AgentClientsServlet`, `ClientService` : recherche, nombre de contrats, fiche avec contrats et sinistres)
- [x] Admin : gérer les utilisateurs et les rôles (`/admin/users` : activer, désactiver, changer le rôle)
- [x] Permettre à un agent ou un admin de corriger le nom d'un utilisateur (`UserService.updateName` : fiche client de `/agent/clients`, `/admin/users/edit`)
- [x] Admin : consulter l'ensemble des contrats (`/admin/contracts`)
- [x] Admin : consulter l'ensemble des sinistres (`/admin/claims` sur `AgentClaimsServlet` : liste avec recherche et onglets par statut, détail, pièces, historique, commentaires en lecture ; actions réservées à l'agent dans la JSP et dans `ClaimService` par `CLAIM_UPDATE` ; `PolicyTest.adminReadsAllClaimsButDoesNotProcessThem`) → reste à tester, voir « Tests à faire dans Tomcat (sinistres) », partie Admin
- [x] Client : consulter l'historique de ses demandes (`/client/history`, `ClientHistoryServlet`, `HistoryService` : contrats et sinistres créés + conversation avec l'assistant ; `HistoryServiceTest`)

## Étape 1 — Contrats (2.2)

Le cœur de l'application : sinistres, paiements et garanties dépendent tous d'un contrat.

- [x] Agent : créer un contrat (`ContractFormServlet`, numéro `CTR-AAAA-NNNN`, prime = prix de base par défaut)
- [x] Agent : modifier un contrat (client et type seulement tant que EN_ATTENTE ; statut via les actions)
- [x] Agent : rechercher un contrat (numéro ou client, statut, type)
- [x] Agent / admin : consulter le détail d'un contrat (`/agent/contracts?id=`, historique de renouvellement et résiliation)
- [x] Agent : activer / suspendre un contrat (boutons de la liste, `ContractActionServlet`)
- [x] Agent : résilier un contrat (page de confirmation avec date + motif)
- [x] Agent : renouveler un contrat (nouveau contrat EN_ATTENTE lié à `contrat_precedent_id`, une seule fois)
- [x] Client : consulter ses contrats, avec filtre par statut (onglets)
- [x] Client : consulter le détail d'un contrat et ses garanties
- [x] Charger ce que les pages affichent (`join fetch` dans `ContractDao.findById` ; les listes n'affichent pas les garanties)
- [x] Passer automatiquement en EXPIRE les contrats dont la date de fin est dépassée (`ContractExpirationListener`, toutes les heures)
- [x] Décider : prime **mensuelle** (une échéance par mois à générer à l'étape 3)

### Tests à faire dans Tomcat (contrats)

Compte agent `agent@assurance.local`, puis admin et client. Redémarrer Tomcat avant.

Liste et recherche (`/agent/contracts`)
- [ ] Le contrat de démo s'affiche, avec le statut en libellé (ACTIF, EN ATTENTE…)
- [ ] Recherche par numéro, par nom de client, par email ; filtre par statut ; filtre par type ; « Réinitialiser »
- [ ] Le numéro de contrat ouvre la page de détail

Création (`+ Nouveau contrat`)
- [ ] Choisir un type pré-remplit la prime et coche ses garanties ; les autres garanties sont grisées
- [ ] Changer la date de début décale la date de fin (+1 an − 1 jour)
- [ ] Erreurs affichées : aucun client, aucun type, fin avant début, prime négative ou texte, aucune garantie
- [ ] Création valide : message « Contrat CTR-2026-000X créé (en attente) », numéro qui s'incrémente

Modification
- [ ] Contrat en attente : client, type, dates, prime et garanties modifiables
- [ ] Contrat actif ou suspendu : client et type en lecture seule, le reste modifiable, pas d'erreur 500
- [ ] Prime laissée vide : l'ancienne prime est conservée
- [ ] Contrat résilié ou expiré : pas de bouton « Modifier » ; l'enregistrement est refusé

Actions
- [ ] Activer un contrat en attente, puis suspendre, puis réactiver
- [ ] Renouveler un contrat actif : nouveau contrat en attente du lendemain de la fin, ouvert en modification
- [ ] Renouveler une 2e fois le même contrat : message d'erreur « déjà renouvelé »
- [ ] Détail : liens « Renouvelle » / « Renouvelé par » entre les deux contrats
- [ ] Résilier : page de confirmation, motif obligatoire, date hors période refusée, puis statut RÉSILIÉ avec date et motif dans le détail

Expiration automatique
- [ ] Dans MySQL, mettre une date de fin passée à un contrat actif, redémarrer, attendre 1 minute : le contrat passe EXPIRÉ

Admin et client
- [ ] Admin `/admin/contracts` : liste et détail visibles, aucun bouton d'action
- [ ] Client `/client/contracts` : seulement ses contrats, onglets par statut, détail avec garanties
- [ ] Client : `/client/contracts?id=` d'un contrat d'un autre client renvoie une 404
- [ ] Lancer `mvn test` (tests unitaires des contrats : création, modification, actions)

## Étape 2 — Sinistres (2.5)

- [x] Client : déclarer un sinistre depuis son espace (`ClientClaimsServlet`, numéro `SIN-AAAA-NNNNNN`, contrat ACTIF obligatoire)
- [x] Client : joindre des pièces justificatives (upload) : à la déclaration ou depuis le suivi tant que DECLARE / EN_ANALYSE ; PDF, JPG, PNG reconnus à leur contenu ; 5 Mo et 5 fichiers max ; stockées hors du WAR (`uploads.dir`, défaut `~/assurcy2000-uploads`) ; téléchargement contrôlé (`?attachment=ID`) ; `ClaimAttachmentTest`, `AttachmentStorageTest`
- [x] Client : suivre l'état d'un sinistre (frise « Avancement » + carte « Historique »)
- [x] Agent : consulter les déclarations (`/agent/claims`, liste et détail)
- [x] Agent : changer le statut (DECLARE → EN_ANALYSE → ACCEPTE / REFUSE → INDEMNISE, transitions contrôlées dans `ClaimService`)
- [x] Agent : ajouter des commentaires (notes internes : lisibles par l'agent et l'admin, pas par le client ; `ClaimCommentTest`)
- [x] Agent : enregistrer le montant d'indemnisation (`ClaimService.indemnify`, seulement depuis ACCEPTE)
- [x] Enregistrer chaque changement de statut dans `historique_sinistre` (`ClaimService.record`, affiché sur le détail client et agent, testé dans `ClaimHistoryTest`)
- [x] Accents corrects dans les fragments `.jspf` (`<jsp-config>` UTF-8 dans `web.xml`)

### Tests à faire dans Tomcat (sinistres)

Avant : `mvn test`, puis arrêter Tomcat, vider le dossier `work` (`.smarttomcat/AssurCY2000/`) et relancer. Compte client `client@assurance.local`, puis `agent@` et `admin@`.

Déclaration (client, `/client/claims?new=1`)
- [ ] Seuls les contrats ACTIFS du client sont proposés
- [ ] Erreurs affichées sous chaque champ : aucun contrat, date vide ou dans le futur, date avant le début du contrat, aucun type, description de moins de 10 caractères, montant négatif ou en lettres
- [ ] Après une erreur, les champs remplis sont conservés et un message demande de rechoisir les fichiers
- [ ] Déclaration valide sans fichier : message « Sinistre SIN-2026-… déclaré. », ouverture du suivi, statut DÉCLARÉ
- [ ] Le numéro s'incrémente à chaque déclaration

Pièces justificatives (client)
- [ ] Déclarer avec 2 fichiers (un PDF et une photo) : ils apparaissent dans « Pièces justificatives » et le récapitulatif affiche « 2 »
- [ ] Glisser-déposer dans la zone, puis « Retirer » un fichier avant l'envoi
- [ ] Un fichier `.txt` renommé en `.pdf` est refusé (« n'est pas un PDF, un JPG ou un PNG »), rien n'est enregistré
- [ ] Un fichier de plus de 5 Mo ou un 6e fichier est bloqué par le navigateur avant l'envoi
- [ ] Depuis le suivi : ajouter des pièces jusqu'à 5 au total, puis le formulaire disparaît (« maximum de 5 pièces »)
- [ ] Cliquer sur une pièce l'ouvre dans un nouvel onglet, avec le bon nom de fichier (essayer un nom avec accents et espaces)
- [ ] Une fois le sinistre ACCEPTÉ, REFUSÉ ou INDEMNISÉ : plus de formulaire d'ajout
- [ ] Les fichiers sont bien dans `~/assurcy2000-uploads/claims/<id>/` et toujours là après un redéploiement

Suivi (client, `/client/claims`)
- [ ] La liste ne montre que ses propres sinistres
- [ ] Le détail affiche la frise « Avancement », la carte « Historique » (Déclaration · date · heure · nom) et les pièces
- [ ] Aucun commentaire de l'agent n'est visible côté client
- [ ] `/client/claims?id=` d'un sinistre d'un autre client renvoie une 404
- [ ] `/client/claims?attachment=` d'une pièce d'un autre client renvoie une 404

Traitement (agent, `/agent/claims`)
- [ ] La liste affiche toutes les déclarations ; le détail affiche contrat, client, description, pièces, historique
- [ ] DÉCLARÉ → EN ANALYSE → ACCEPTÉ : chaque étape ajoute une ligne dans « Historique » avec le nom de l'agent
- [ ] Un autre sinistre : EN ANALYSE → REFUSÉ ; plus aucune action possible ensuite
- [ ] Indemniser un sinistre ACCEPTÉ : montant vide, négatif ou en lettres refusé ; montant valide → INDEMNISÉ, montant et date affichés côté agent et côté client
- [ ] Commentaires : publier deux commentaires (dont un sur plusieurs lignes), en envoyer un vide → erreur, le texte tapé est conservé
- [ ] Commenter un sinistre déjà REFUSÉ ou INDEMNISÉ fonctionne

Admin (`admin@assurance.local`, `/admin/claims`)
- [ ] Lien « Tous les sinistres » de la sidebar actif ; titre « Tous les sinistres », mention « Administration »
- [ ] Liste de tous les sinistres (de tous les clients) ; recherche par numéro, client ou contrat ; onglets par statut avec compteurs
- [ ] Le numéro ouvre le détail en restant sous `/admin/claims?id=` (lien « ← Tous les sinistres » qui revient à la liste admin)
- [ ] Détail : contrat, client, description, historique, pièces ouvrables (`/admin/claims?attachment=`), commentaires des agents visibles
- [ ] Aucun bouton de changement de statut, d'indemnisation ni formulaire de commentaire
- [ ] Un POST forgé (changer le statut depuis l'inspecteur ou avec curl) → 403, rien ne change
- [ ] `?id=999` et `?attachment=999` → 404 ; agent sur `/admin/claims` → 403 ; client → 403

Vérification des saisies par les servlets (après la correction du 28/11)
- [ ] Déclaration avec tous les champs faux à la fois → toutes les erreurs affichées ensemble, code 400, rien d'enregistré
- [ ] Date du sinistre vide → « La date du sinistre est obligatoire. » ; date avant le début du contrat → toujours refusée (vérifiée par le service)
- [ ] Ajout de pièces depuis le suivi sans fichier → « Choisissez au moins un fichier. »
- [ ] Agent : indemnisation à 0 ou au-dessus de 10 000 000 € → message sous le champ ; statut forgé (`status=PIRATE` avec l'inspecteur) → « Choisissez le nouveau statut. »

Affichage
- [ ] Accents corrects partout (frise, historique, commentaires, messages), y compris dans un nom de fichier joint

## Étape 3 — Paiements (2.6)

Prime **mensuelle** : une échéance par mois de la période (le même jour que la date de début, ramené au dernier jour des mois courts), montant = prime du contrat.

- [x] Générer les échéances à la création et au renouvellement d'un contrat (`PaymentSchedule.generate`, appelé dans `ContractService.create` et `renew`, même transaction)
- [x] Modification d'un contrat : l'échéancier suit les dates et la prime (`PaymentSchedule.resync` ; échéances payées ou annulées inchangées)
- [x] Résiliation : les échéances non payées après la date de résiliation passent ANNULE (`PaymentSchedule.cancelAfter`)
- [x] Échéances dépassées des contrats ACTIF / SUSPENDU passées EN_RETARD toutes les heures (`PaymentService.markOverdue`, dans `ContractExpirationListener`)
- [x] Échéancier des contrats créés avant l'Étape 3 : migration `V5__echeances_contrats_existants.sql` (contrats en cours sans aucun paiement)
- [x] Consulter les paiements effectués (onglet « Effectués », `?filter=paid`)
- [x] Consulter les paiements en attente et les échéances (onglets « En attente » = dus à ce jour, retards compris, `?filter=pending` ; « Échéances » = à venir, `?filter=due`)
- [x] Consulter l'historique des paiements (onglet par défaut : payés, annulés et déjà dus, du plus récent au plus ancien)
- [x] Client : `/client/payments` (ses paiements, `&contractId=` pour un contrat, résumé : total payé, à régler, retards, prochaine échéance)
- [x] Agent : `/agent/payments` (tous les paiements, recherche par n° de contrat, nom ou email du client, `&contractId=` depuis le détail d'un contrat)
- [x] Agent : enregistrer un paiement (formulaire : échéance, date, mode ; bouton « Encaisser » sur chaque ligne non payée ; `PaymentService.markPaid` : ni payée ni annulée, date pas dans le futur)
- [x] Admin : `/admin/payments` (consultation seule, lien « Tous les paiements » dans la sidebar)
- [x] Tests unitaires : `PaymentScheduleTest`, `PaymentServiceTest`, échéancier vérifié dans `ContractCreationTest`, `ContractUpdateTest`, `ContractActionsTest`

### Tests à faire dans Tomcat (paiements)

Avant : `mvn test` (vert, dont `PaymentScheduleTest` et `PaymentServiceTest`), puis arrêter Tomcat, vider le dossier `work` (`.smarttomcat/AssurCY2000/`) et relancer. Au démarrage, Flyway doit appliquer `V5__echeances_contrats_existants` (visible dans la console, et `SELECT version, success FROM flyway_schema_history;` montre la ligne 5 à 1). Comptes `agent@`, `client@`, `admin@assurance.local` / `Assurance2026!`.

Migration V5 (contrats créés avant l'Étape 3)
- [ ] Un contrat en attente, actif ou suspendu créé avant aujourd'hui a maintenant une échéance par mois (`/agent/payments?contractId=…`)
- [ ] Le contrat de démo CT-2026-0001 garde ses 4 paiements d'origine (pas de doublon)
- [ ] Les contrats résiliés ou expirés n'ont pas reçu d'échéances

Échéancier (agent `agent@assurance.local`)
- [ ] Créer un contrat pour `client@` du 01/01/2027 au 31/12/2027, prime 30 € → détail du contrat, « Voir l'échéancier de ce contrat » : 12 échéances de 30,00 €, du 01/01/2027 au 01/12/2027, toutes « En attente », dans l'onglet « Échéances »
- [ ] Créer un contrat qui commence le 31 d'un mois → échéances le 31, ou le dernier jour des mois courts (28/02, 30/04…)
- [ ] Créer un contrat qui a commencé il y a 3 mois, sans l'activer → ses échéances passées sont dans « En attente » mais pas « En retard »
- [ ] L'activer, puis redémarrer Tomcat et attendre 1 min (la tâche tourne au démarrage + 1 min, puis toutes les heures) → les échéances passées sont « En retard », le résumé indique le nombre de retards
- [ ] Modifier la prime (30 → 35 €) → les échéances à venir passent à 35 €, celles déjà payées ou en retard gardent 30 €
- [ ] Raccourcir la période à 6 mois → seules 6 échéances restent (plus les éventuelles payées hors période)
- [ ] Allonger la période → les nouveaux mois ont leur échéance
- [ ] Renouveler un contrat → le nouveau contrat (statut en attente) a ses 12 échéances, l'ancien garde les siennes
- [ ] Résilier avec une date de résiliation aujourd'hui → les échéances après aujourd'hui sont « Annulé », celles d'avant restent à payer

Liste et recherche (agent, `/agent/payments`)
- [ ] Lien « Paiements » de la sidebar actif sur la page
- [ ] Onglet par défaut « Historique » : payés, annulés et déjà dus, du plus récent au plus ancien ; aucune échéance future
- [ ] « Effectués » : uniquement les payés ; « En attente » : dus à ce jour non payés, le plus ancien d'abord ; « Échéances » : à venir, la plus proche d'abord
- [ ] Recherche par numéro de contrat, par nom (« martin »), par prénom + nom, par email ; insensible à la casse ; la recherche reste quand on change d'onglet ; « Réinitialiser » l'enlève
- [ ] Le numéro de contrat d'une ligne ouvre le détail du contrat ; « ← Retour au contrat » depuis l'échéancier d'un contrat
- [ ] Résumé en haut : total payé, à régler, nombre de retards, prochaine échéance (date, montant, contrat)

Encaissement (agent)
- [ ] Formulaire « Enregistrer un paiement » : la liste propose les échéances non payées jusqu'au mois prochain, les retards marqués « en retard » ; la date est pré-remplie à aujourd'hui
- [ ] Bouton « Encaisser » sur une ligne → la page descend au formulaire, l'échéance est pré-sélectionnée et la ligne surlignée (marche aussi pour une échéance dans 6 mois)
- [ ] Pas de bouton « Encaisser » sur une ligne payée ou annulée
- [ ] Envoyer sans échéance, sans mode, avec une date vide → messages sous les champs, code 400, choix conservés
- [ ] Date de demain (en retirant le `max` avec l'inspecteur) → « La date de paiement ne peut pas être dans le futur. »
- [ ] Paiement valide (virement, aujourd'hui) → message « Paiement de l'échéance du … enregistré pour CTR-… », onglet « Effectués » : statut « Effectué », date, mode « Virement » ; le total payé augmente
- [ ] Payer une échéance en retard → elle passe « Effectué », le nombre de retards baisse
- [ ] Même échéance encaissée deux fois (deux onglets ouverts, ou F5 sur le POST) → « Cette échéance est déjà payée. »
- [ ] Échéance annulée (contrat résilié) envoyée via l'inspecteur → « Cette échéance est annulée (contrat résilié). »
- [ ] Sans jeton CSRF (supprimé avec l'inspecteur) → refus (403)

Client (`client@assurance.local`)
- [ ] Sidebar « Paiements » → `/client/payments` : uniquement ses paiements, mêmes onglets et résumé, aucune colonne Client, aucun formulaire ni bouton « Encaisser »
- [ ] `/client/contracts` : bouton « Paiements » d'un contrat, et « Voir les paiements de ce contrat » sur le détail → seulement ce contrat, le titre affiche son numéro, les onglets gardent le contrat
- [ ] Un paiement encaissé par l'agent apparaît dans « Effectués » avec la date et le mode
- [ ] `/client/payments?contractId=` d'un contrat d'un autre client → 404 ; `?contractId=abc` → 404
- [ ] `/agent/payments` en tant que client → 403 ; un POST forgé vers `/agent/payments` → 403
- [ ] `?filter=nimportequoi` → onglet Historique, pas d'erreur

Admin (`admin@assurance.local`)
- [ ] Sidebar « Tous les paiements » → `/admin/payments` : tous les paiements, onglets, recherche, résumé
- [ ] Aucun formulaire, aucune colonne « Action » ; un POST forgé vers `/agent/payments` → refusé (403)
- [ ] Détail d'un contrat côté admin → « Voir l'échéancier de ce contrat » ouvre `/admin/payments?contractId=…`

Affichage
- [ ] Statuts en libellé avec la bonne couleur : En attente, Effectué, En retard, Annulé (gris)
- [ ] Montants au format français (30,00 €), dates en jj/mm/aaaa, accents corrects
- [ ] Page lisible sur un écran étroit (tableau qui défile, formulaire sous la liste)

## Étape 4 — Garanties et types d'assurance (2.3, 2.4)

- [x] Plafond et franchise des garanties : migration `V6__garantie_plafond_franchise.sql` (colonnes facultatives, valeurs de démo), champs `coverageLimit` / `deductible` dans `Guarantee`
- [x] Agent : créer, modifier, supprimer une garantie (`/agent/guarantees`, `AgentGuaranteesServlet`, `GuaranteeService` : nom unique, franchise ≤ plafond, suppression refusée si un contrat l'utilise ; `GuaranteeServiceTest`)
- [x] Agent : associer une garantie à un contrat (formulaire de contrat de l'Étape 1 : garanties à cocher parmi celles du type, `ContractService.guaranteesOfType`) → reste à tester, voir « Association au contrat » ci-dessous
- [x] Client : consulter ses garanties (`/client/guarantees`, `ClientGuaranteesServlet`, `ContractService.listOwnWithGuarantees` : contrats actifs, suspendus et en attente, garanties par ordre alphabétique ; `ClientGuaranteesTest`)
- [x] Admin : gérer les types d'assurance (nom, description, prix de base, garanties disponibles) : `/admin/types`, `AdminTypesServlet`, `InsuranceTypeService` (nom unique, au moins une garantie, activer / désactiver, suppression refusée si des contrats l'utilisent ; `InsuranceTypeServiceTest`)
- [x] Ajouter un type sans toucher au code : les types viennent de la base, pas d'un `enum` (un type créé dans `/admin/types` est aussitôt proposé dans le formulaire de contrat)

### Tests à faire dans Tomcat (garanties et types d'assurance)

Avant : `mvn test` (vert, dont `GuaranteeServiceTest`, `ClientGuaranteesTest` et `InsuranceTypeServiceTest`), puis arrêter Tomcat, vider le dossier `work` (`.smarttomcat/AssurCY2000/`) et relancer. Flyway doit appliquer `V6__garantie_plafond_franchise` (visible dans la console ; `SELECT version, success FROM flyway_schema_history;` montre la ligne 6 à 1 ; `SELECT nom, plafond, franchise FROM garantie;` montre les valeurs de démo). Comptes `agent@`, `client@`, `admin@assurance.local` / `Assurance2026!`.

Garanties : liste (agent, `/agent/guarantees`)
- [ ] Lien « Garanties » de la sidebar actif ; 6 garanties triées par nom, plafond et franchise au format français, « — » / « Aucune » quand c'est vide
- [ ] Colonne « Contrats » : Responsabilité civile, Vol et Accident à 1 au moins (contrat de démo) ; leur bouton « Supprimer » est grisé

Garanties : création
- [ ] Créer « Bris de glace », plafond 2000, franchise 100 → message « Garantie « Bris de glace » créée. », elle apparaît dans la liste
- [ ] Nom vide, nom déjà pris (« Vol »), franchise 500 avec plafond 100, montant en lettres → message sous le champ, code 400, saisies conservées
- [ ] Plafond et franchise vides → acceptés
- [ ] La nouvelle garantie est proposée dans le formulaire de contrat (`+ Nouveau contrat`)

Garanties : modification
- [ ] « Modifier » : ligne surlignée, formulaire pré-rempli, titre « Modifier la garantie », message « Utilisée par N contrat(s) » si c'est le cas
- [ ] Renommer en gardant le même nom → accepté ; renommer en « Incendie » → « Une garantie porte déjà ce nom. »
- [ ] Après une erreur, les valeurs tapées restent (pas celles de la base) ; « Annuler » revient en création
- [ ] Modifier le plafond de « Vol » → le nouveau montant apparaît aussi dans `/client/guarantees` et le détail du contrat de démo (garantie partagée)
- [ ] `?id=999` et `?id=abc` → 404

Garanties : suppression
- [ ] Supprimer « Bris de glace » : confirmation, puis « Garantie « Bris de glace » supprimée. »
- [ ] Garantie utilisée envoyée quand même (bouton réactivé avec l'inspecteur) → message d'erreur, rien n'est supprimé
- [ ] Sans jeton CSRF → 403

Garanties : droits
- [ ] Client et admin sur `/agent/guarantees` → 403

Association au contrat (agent, formulaire de contrat)
- [ ] Créer un contrat avec 2 garanties, puis en retirer une en modification → le détail du contrat affiche les bonnes garanties
- [ ] La colonne « Contrats » de `/agent/guarantees` suit (+1 puis −1)
- [ ] Les garanties du détail d'un contrat (agent et client) sont par ordre alphabétique
- [ ] Une garantie qui n'est pas proposée par le type est grisée dans le formulaire ; envoyée quand même (inspecteur) → « Certaines garanties choisies ne sont pas proposées pour … »

Client (`client@assurance.local`, `/client/guarantees`)
- [ ] Lien « Mes garanties » de la sidebar actif ; une carte par contrat actif, suspendu ou en attente, avec type, numéro (lien vers le détail) et statut
- [ ] Garanties par ordre alphabétique, plafond et franchise affichés (« — » / « Aucune » si vides)
- [ ] Contrats expirés ou résiliés absents ; aucun contrat d'un autre client
- [ ] Un client sans contrat en cours voit « Aucune garantie : vous n'avez pas de contrat en cours. »
- [ ] Agent et admin sur `/client/guarantees` → 403

Types d'assurance (admin `admin@assurance.local`, `/admin/types`)
- [ ] Lien « Types d'assurance » de la sidebar actif ; 5 types par nom, badge ACTIF, prix « /mois », nombre de contrats, garanties en étiquettes par ordre alphabétique
- [ ] « Supprimer » absent sur un type utilisé par des contrats (Assurance automobile avec le contrat de démo)
- [ ] Créer « Assurance animaux », 15 €, garanties Responsabilité civile + Accident → « Type « Assurance animaux » créé. »
- [ ] Erreurs : nom vide, nom déjà pris, prix vide / négatif / en lettres, aucune garantie cochée → messages sous les champs, code 400, saisies et cases cochées conservées
- [ ] Modifier : formulaire pré-rempli avec les cases du type cochées ; décocher une garantie et enregistrer → étiquettes à jour
- [ ] Agent, `+ Nouveau contrat` : « Assurance animaux » est proposée, avec son prix et ses garanties (sans redémarrer)
- [ ] Retirer une garantie d'un type utilisé (ex. « Vol » de Assurance automobile) → le contrat de démo la garde dans son détail ; elle n'est plus proposée dans le formulaire de contrat (puis remettre « Vol »)
- [ ] Désactiver « Assurance animaux » → badge DÉSACTIVÉ, plus proposée dans le formulaire de contrat ; toujours dans le filtre par type de `/agent/contracts` ; « Activer » la remet
- [ ] Supprimer « Assurance animaux » (aucun contrat) → « Type « Assurance animaux » supprimé. » ; ses garanties existent toujours dans `/agent/guarantees`
- [ ] Désactiver « Assurance automobile » (utilisée) : le contrat de démo (actif) reste consultable et modifiable (dates, prime, garanties) ; un contrat en attente de ce type doit changer de type pour être enregistré (le type désactivé n'est plus dans la liste) ; réactiver ensuite
- [ ] `?id=999`, `?id=abc` → 404 ; agent et client sur `/admin/types` → 403 ; sans jeton CSRF → 403
- [ ] Accents corrects partout (noms de types et de garanties, messages, étiquettes)

## Étape 5 — Statistiques (2.8)

- [x] Statistiques clients : total, nouveaux, répartition par type d'assurance (`StatsService`, `StatsJdbcDao`)
- [x] Statistiques contrats : répartition par statut, contrats qui commencent chaque mois (graphique)
- [x] Statistiques financières : primes des contrats actifs, paiements encaissés, indemnisations versées, encaissements et indemnisations par mois (graphique)
- [x] Accessible à l'agent et à l'admin, pas au client (`/agent/stats`, `/admin/stats`, permission `STATS_READ`)
- [x] Admin uniquement : indicateurs de performance (taux d'acceptation, délai moyen de traitement, ratio indemnisations / encaissements) : section entourée de `<c:if test="${isAdmin}">` dans `admin/stats.jsp` ; lien de la sidebar admin corrigé (`/admin/stats#indicators`)

### Tests à faire dans Tomcat (statistiques)

- [ ] Agent `/agent/stats` et admin `/admin/stats` : la page s'affiche sans erreur 500, titres « Statistiques » / « Tableau de bord global »
- [ ] Les chiffres collent à la base : nombre de clients, nouveaux clients, clients par type, contrats par statut (comparer avec phpMyAdmin)
- [ ] Après avoir enregistré un paiement et indemnisé un sinistre : encaissements, indemnisations et graphiques par mois mis à jour
- [ ] Base sans paiement ni sinistre indemnisé : « — » à la place des taux et du délai, pas de division par zéro
- [ ] Indicateurs de performance visibles par l'admin seulement (après correction) ; lien de la sidebar admin qui descend bien jusqu'à la section
- [ ] Client sur `/agent/stats` ou `/admin/stats` → 403

## Étape 6 — Assistant IA (2.7)

En dernier : il réutilise les services des étapes précédentes pour fournir au modèle les données du client.

- [x] Choisir l'API d'IA et gérer la clé (hors du dépôt Git) : Gemini (`google-genai`, modèle `gemini-3.1-flash-lite`, `GeminiAiClient`) ; clé dans `GEMINI_API_KEY` ou `gemini.api.key` de `db.properties` (ignoré par Git)
- [x] Page de discussion client, messages enregistrés dans `message_ia` (`/client/assistant`, `AssistantServlet` en JSON + `assistant.js`, `AssistantService` : 500 caractères max, 1 message / 2 s, 10 derniers messages envoyés au modèle, CSRF par en-tête)
- [x] Répondre à : mes contrats actifs, garanties de mon assurance auto, comment déclarer un sinistre, documents à fournir, différence entre deux garanties, où en est mon dernier sinistre (le modèle reçoit les contrats avec garanties, plafonds et franchises, les 10 derniers sinistres, les paiements, et les règles du site) → reste à tester, voir ci-dessous
- [ ] **À corriger avant le rendu** : le prompt système de `AssistantService` contient une porte dérobée (« SAUF si le client commence son message par -BYPASS ») qui permet à n'importe quel client de lever les règles de l'assistant ; supprimer cette phrase
- [x] README, point 5 : instructions de la clé reformulées (`gemini.api.key` dans `db.properties` ou variable `GEMINI_API_KEY`, prioritaire) ; ligne d'exemple ajoutée dans `db.properties.example`

### Tests à faire dans Tomcat (assistant IA)

Avant : clé Gemini configurée (`GEMINI_API_KEY` dans la configuration Tomcat d'IntelliJ, ou `gemini.api.key` dans `db.properties`), redémarrer Tomcat. Compte `client@assurance.local`.

- [ ] `/client/assistant` : le résumé (contrats, garanties, sinistres, paiements, agent référent) correspond au compte
- [ ] « Quels sont mes contrats actifs ? » → les bons numéros, rien d'inventé
- [ ] « Quelles sont les garanties de mon assurance auto ? » → mêmes garanties, plafonds et franchises que `/client/guarantees`
- [ ] « Comment déclarer un sinistre ? » et « Quels documents fournir ? » → menu Sinistres, PDF/JPG/PNG, 5 Mo, 5 fichiers
- [ ] « Quelle différence entre Vol et Incendie ? » → s'appuie sur les plafonds et franchises du contrat
- [ ] « Où en est mon dernier sinistre ? » → numéro et statut du sinistre le plus récent
- [ ] Question sur un autre client, ou demande d'accepter un sinistre → refus poli, renvoi vers l'agent
- [ ] Message commençant par « -BYPASS » → l'assistant garde ses règles (après correction)
- [ ] Message vide, plus de 500 caractères, deux messages en moins de 2 s → message d'erreur, rien d'enregistré
- [ ] Sans clé : « L'assistant n'est pas encore disponible. », pas d'erreur 500
- [ ] Les échanges apparaissent dans `message_ia` et dans `/client/history` ; après rechargement, l'assistant se souvient des derniers messages
- [ ] Agent et admin sur `/client/assistant` → 403 ; POST sans en-tête `X-CSRF-Token` → 403

### Tests à faire dans Tomcat (tableau de bord et historique client)

- [ ] Après connexion, le client arrive sur `/client/dashboard` : nombre de contrats (actifs, en attente), primes, sinistres en cours, dernier sinistre, prochain paiement, derniers paiements
- [ ] Client sans contrat : la page s'affiche avec des zéros / « aucun », pas d'erreur 500
- [ ] `/client/history` : contrats et sinistres du client du plus récent au plus ancien, et sa conversation avec l'assistant ; rien d'un autre client
- [ ] L'ancienne adresse `/client/historique` renvoie une 404 (normal après le renommage)
- [ ] Agent et admin sur ces pages → 403

## Architecture et contraintes (sections 3 et 4)

- [x] MVC : servlets (contrôleurs), JSP (vues), entités Java (modèle)
- [x] Persistance avec Hibernate et JDBC (`StatsJdbcDao`)
- [x] Cohérence des données : transactions ouvertes par les services (`TransactionRunner`)
- [x] Authentification et accès par rôle (`AuthFilter`, `RoleFilter`, `Policy`)
- [x] Toutes les saisies vérifiées par les servlets avant l'enregistrement : inscription, profil, contrats (création, modification, résiliation), paiements, garanties, types, utilisateurs, et sinistres (déclaration et pièces dans `ClientClaimsServlet` ; statut, indemnisation et commentaire dans `AgentClaimsServlet`). Règles de format dans `Validation` (testées dans `ValidationTest`), réutilisées par les services comme filet de sécurité ; les services ne vérifient en plus que ce qui demande la base. À respecter pour tout nouveau formulaire
- [ ] CRUD complet par servlets pour chaque entité gérée (contrats, garanties, types, sinistres, paiements, utilisateurs)

## Corrections connues

Pour chaque page branchée : même URL dans la servlet et dans les liens, mêmes noms d'attributs, la JSP lit les vrais getters de l'entité, et le DAO charge tout ce que la page affiche.

- [x] Aligner les URLs des servlets, de la sidebar et des JSP (tout est passé en anglais : `/agent/contracts`, `/agent/claims`, `/admin/users`…)
- [x] `homePath()` de l'agent pointe vers `/agent/claims`, qui a une servlet
- [x] `Views.STATS` pointe vers `admin/stats.jsp`, qui existe
- [x] `Views.AGENT_CLAIM` pointe vers `agent/claims.jsp`, qui existe
- [x] `/client/dashboard` a sa servlet (`ClientDashboardServlet`, `ClientDashboardService` : contrats, primes, sinistres en cours, dernier sinistre, prochain paiement ; `ClientDashboardServiceTest`) et redevient la page d'accueil du client
- [x] `/client/historique` renommée en `/client/history` (servlet, sidebar, commentaire de `history.jsp`) : toutes les URLs sont en anglais

## Avant le rendu

- [x] Supprimer `views/client/profil.jsp` (remplacé par `common/profile.jsp`)
- [x] Supprimer `FieldException.java`
- [ ] Retirer `PreviewServlet` et l'exception `/preview` dans `AuthFilter` → l'exception est retirée (la page demande maintenant d'être connecté), mais `src/main/java/com/dev/PreviewServlet.java` est toujours là, mappé sur `/preview` : supprimer le fichier
- [x] Garder une seule ligne `hibernate.show_sql` dans `hibernate.cfg.xml`
- [ ] Retirer la phrase « -BYPASS » du prompt de l'assistant (voir Étape 6)
- [ ] Données de démo (V3) : le contrat CT-2026-0001 a une prime de 450 € « mensuelle » mais 4 paiements trimestriels de 112,50 € ; à corriger dans `db/script.sql` / une nouvelle migration si la démo doit être cohérente
- [ ] Partie conception : MCD ou diagramme de classes
- [ ] Rendre le dépôt GitHub public à la date du dépôt final
- [ ] Refaire l'application avec Spring Boot
