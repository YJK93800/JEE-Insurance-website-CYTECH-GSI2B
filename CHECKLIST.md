# Projet JEE AssurCY2000 — Checklist

Mise à jour le 24/11/2026. Cochez les cases dans vos commits au fur et à mesure.

## Étape 0 — Authentification et profil (section 2.1)

Codé ; reste à tester dans Tomcat avec les comptes de démo (`client@`, `agent@`, `admin@assurance.local` / `Assurance2026!`).

- [x] Créer un compte (`RegisterServlet`, validation dans `Validation`)
- [x] Se connecter / se déconnecter
- [x] Modifier ses coordonnées : téléphone et adresse (`updateInfos`, `UpdateProfileRequest`)
- [x] Changer son mot de passe (vérification de l'ancien dans `UserService`)
- [x] Identité verrouillée sur `/profil` (nom, prénom, email, date de naissance)
- [ ] Tester l'inscription : champ vide, moins de 18 ans, email déjà pris, puis inscription valide
- [ ] Tester la connexion avec les 3 comptes de démo
- [ ] Tester `/profil` : modifier téléphone et adresse, puis changer le mot de passe (mauvais puis bon mot de passe actuel)
- [ ] Lancer `mvn test` en local

## Utilisateurs côté agent et admin (2.1)

- [ ] Agent : consulter les clients (`agent/clients.jsp`, pas encore de servlet)
- [ ] Admin : gérer les utilisateurs et les rôles (`/admin/users` : activer, désactiver, changer le rôle)
- [ ] Permettre à un agent ou un admin de corriger le nom d'un utilisateur (le profil ne le permet plus)
- [x] Admin : consulter l'ensemble des contrats (`/admin/contracts`)
- [ ] Admin : consulter l'ensemble des sinistres (`/admin/claims`)
- [ ] Client : consulter l'historique de ses demandes (`client/history.jsp`)

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

- [ ] Client : déclarer un sinistre depuis son espace
- [ ] Client : joindre des pièces justificatives (upload)
- [ ] Client : suivre l'état d'un sinistre
- [ ] Agent : consulter les déclarations
- [ ] Agent : changer le statut (DECLARE → EN_ANALYSE → ACCEPTE / REFUSE → INDEMNISE)
- [ ] Agent : ajouter des commentaires
- [ ] Agent : enregistrer le montant d'indemnisation
- [ ] Enregistrer chaque changement de statut dans `historique_sinistre`

## Étape 3 — Paiements (2.6)

- [ ] Consulter les paiements effectués
- [ ] Consulter les paiements en attente et les échéances
- [ ] Consulter l'historique des paiements
- [ ] Agent : enregistrer un paiement (`PaymentService.markPaid` existe, aucune servlet ne l'appelle)
- [ ] Générer les échéances à la création et au renouvellement d'un contrat

## Étape 4 — Garanties et types d'assurance (2.3, 2.4)

- [ ] Agent : créer, modifier, supprimer une garantie
- [ ] Agent : associer une garantie à un contrat
- [ ] Client : consulter ses garanties
- [ ] Admin : gérer les types d'assurance (nom, description, prix de base, garanties disponibles)
- [ ] Ajouter un type sans toucher au code : les types viennent de la base, pas d'un `enum`

## Étape 5 — Statistiques (2.8)

- [ ] Statistiques clients : total, nouveaux, répartition par type d'assurance
- [ ] Statistiques contrats : actifs, expirés, résiliés, évolution
- [ ] Statistiques financières : chiffre d'affaires, primes, indemnisations, évolution des revenus
- [ ] Accessible à l'agent et à l'admin, pas au client
- [ ] Admin uniquement : indicateurs de performance (taux d'acceptation, délai moyen de traitement, ratio indemnisations / primes)

## Étape 6 — Assistant IA (2.7)

En dernier : il réutilise les services des étapes précédentes pour fournir au modèle les données du client.

- [ ] Choisir l'API d'IA et gérer la clé (hors du dépôt Git)
- [ ] Page de discussion client, messages enregistrés dans `message_ia`
- [ ] Répondre à : mes contrats actifs, garanties de mon assurance auto, comment déclarer un sinistre, documents à fournir, différence entre deux garanties, où en est mon dernier sinistre

## Architecture et contraintes (sections 3 et 4)

- [x] MVC : servlets (contrôleurs), JSP (vues), entités Java (modèle)
- [x] Persistance avec Hibernate et JDBC (`StatsJdbcDao`)
- [x] Cohérence des données : transactions ouvertes par les services (`TransactionRunner`)
- [x] Authentification et accès par rôle (`AuthFilter`, `RoleFilter`, `Policy`)
- [ ] Toutes les saisies vérifiées par les servlets avant l'enregistrement (fait pour inscription, profil, création de contrat ; à faire pour chaque nouveau formulaire)
- [ ] CRUD complet par servlets pour chaque entité gérée (contrats, garanties, types, sinistres, paiements, utilisateurs)

## Corrections connues

Pour chaque page branchée : même URL dans la servlet et dans les liens, mêmes noms d'attributs, la JSP lit les vrais getters de l'entité, et le DAO charge tout ce que la page affiche.

- [x] Aligner les URLs des servlets, de la sidebar et des JSP (tout est passé en anglais : `/agent/contracts`, `/agent/claims`, `/admin/users`…)
- [x] `homePath()` de l'agent pointe vers `/agent/claims`, qui a une servlet
- [x] `Views.STATS` pointe vers `admin/stats.jsp`, qui existe
- [x] `Views.AGENT_CLAIM` pointe vers `agent/claims.jsp`, qui existe
- [ ] `/client/dashboard` n'a pas de servlet : le client est envoyé sur `/client/contracts` en attendant

## Avant le rendu

- [x] Supprimer `views/client/profil.jsp` (remplacé par `common/profile.jsp`)
- [x] Supprimer `FieldException.java`
- [ ] Retirer `PreviewServlet` et l'exception `/preview` dans `AuthFilter`
- [ ] Garder une seule ligne `hibernate.show_sql` dans `hibernate.cfg.xml` (toujours en double)
- [ ] Partie conception : MCD ou diagramme de classes
- [ ] Rendre le dépôt GitHub public à la date du dépôt final
- [ ] Refaire l'application avec Spring Boot
