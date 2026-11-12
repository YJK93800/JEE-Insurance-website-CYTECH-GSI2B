# JEE-Insurance-website-CYTECH-GSI2B

## Lancer le projet

### Prérequis

- Docker (avec Docker Compose) 
- JDK >= 17
- Tomcat >= 10.1
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

3. **Lancer Tomcat** depuis IntelliJ (la base doit être "healthy" avant).

4. Ouvrir http://localhost:8080/projet-jee

### Outils

- phpMyAdmin (pour voir la base de données) : http://localhost:8081
  — se connecter avec `DB_USER` / `DB_PASSWORD` du `.env`

## Base de données

- MySQL 8 tourne dans Docker, exposé sur le port **3307** de la machine
  (pour ne pas gêner un MySQL déjà installé).
- Les données sont gardées entre deux `docker compose down` / `up` (volume `mysql-data`).
- Repartir d'une base vide : `docker compose down -v && docker compose up -d`.
