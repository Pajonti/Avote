# Avote
Tracking de la vie citoyenne aux parlements : Parlement Européen, Assemblée Nationale et Sénat

Première version : une grille des parlementaires en exercice (recherche, filtre par chambre et par
groupe politique) avec une fiche de détail, alimentée par les données publiques des trois assemblées.

## Architecture

| Dossier  | Rôle |
|----------|------|
| `back/`  | API Spring Boot 4 (Java 25, Maven), migrations Flyway, cache Redis, ingestion des données |
| `front/` | Application Vue 3 + Vite + TypeScript, servie par nginx (qui relaie `/api` vers le back) |

Services de `docker-compose.yml` : `postgres`, `redis`, `back`, `front`. Chaque composant applicatif a
son `Dockerfile`. Le déploiement (GitHub Actions, ArgoCD, OpenBao) vit hors de ce dépôt.

### Sources de données (licences ouvertes)

| Chambre | Source |
|---------|--------|
| Assemblée nationale | `data.assemblee-nationale.fr` : export « députés actifs et mandats actifs » (zip JSON) |
| Sénat | `www.senat.fr/api-senat/senateurs.json` + `data.senat.fr` (`ODSEN_GENERAL.json`) |
| Parlement européen | `data.europarl.europa.eu` : API Open Data, députés en exercice |

L'ingestion tourne tous les jours (`AVOTE_INGESTION_CRON`) et au démarrage si une chambre est vide.
Elle est idempotente (upserts), transactionnelle par chambre, et refuse un import vide ou réduit de
plus de moitié par rapport aux effectifs connus. Le suivi est dans la table `ingestion_run`. Les
collaborateurs parlementaires présents dans l'export de l'Assemblée ne sont volontairement pas importés.

> Une seule instance du back doit tourner : l'exclusion entre imports est locale à l'instance.

### API

- `GET /api/v1/parliamentarians?chamber=AN|SENAT|EP&group=<id>&q=<nom>&page=0&size=24`
- `GET /api/v1/parliamentarians/{id}`
- `GET /api/v1/groups?chamber=…` et `GET /api/v1/chambers`

Les lectures sont mises en cache dans Redis (TTL 1 h, vidé après chaque import). Si Redis tombe,
l'API continue de répondre sans cache.

## Démarrer avec Docker

```bash
cp .env-example .env        # puis remplacer les mots de passe « change-me »
docker compose up -d --build
```

- Application : <http://localhost:8080> (`FRONT_PORT`)
- API directe : <http://localhost:8081> (`BACK_PORT`), santé : `/actuator/health`

Au premier démarrage, l'import des 1 642 parlementaires prend une dizaine de secondes.

## Développer en local

```bash
docker compose up -d postgres redis      # bases seules
cd back && mvn spring-boot:run           # lit ../.env ; écoute sur :8080
cd front && npm install && npm run dev   # http://localhost:5173, /api relayé vers :8080
```

Si le back tourne via compose plutôt que Maven : `VITE_API_PROXY=http://localhost:8081 npm run dev`.

## Tests

```bash
cd back && mvn verify     # unitaires + intégration (Testcontainers : Docker requis)
cd front && npm test && npm run build
```

## Configuration

Toute la configuration locale passe par `.env` (modèle : `.env-example`). `.env` est ignoré par git.
En production, les secrets sont fournis par OpenBao sous forme de variables d'environnement.
