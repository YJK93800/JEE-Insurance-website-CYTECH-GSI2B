# JEE-Insurance-website-CYTECH-GSI2B

## Lancer le projet

### Prérequis

- Docker (avec Docker Compose) 
- JDK >= 20
- Tomcat >= 10
- IntelliJ

1. **Configurer les mots de passe** (une seule fois) :
   ```bash
   cp .env.example .env
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```
   Choisir un mot de passe dans `.env` et mettre **le même** (`DB_USER` / `DB_PASSWORD`) dans `db.properties`.
   Ces deux fichiers sont ignorés par Git.

2. **Démarrer la base** :
   ```bash
   docker compose up -d
   docker compose ps      # attendre que assurance-mysql soit "healthy"
   ```

3. **Lancer Tomcat** depuis IntelliJ. Au démarrage, Flyway crée toutes les tables
   à partir de `src/main/resources/db/migrations/`.

4. Ouvrir http://localhost:8080/projet-jee

### Outils

- phpMyAdmin (pour voir la base de données) : http://localhost:8081

## Base de données

- Le schéma est défini **uniquement** par les scripts Flyway dans `src/main/resources/db/migrations/`.
- **Ne jamais modifier un script déjà poussé sur `main`** : ajouter un nouveau fichier
  `V2__description.sql`, `V3__...`, avec le numéro suivant.
- Repartir d'une base vide : `docker compose down -v && docker compose up -d`.
