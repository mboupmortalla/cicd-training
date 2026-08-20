# Order Management API

API REST de gestion de commandes, servant de projet fil rouge à la mise en
place d'une chaîne DevSecOps complète : tests, analyse statique, scan de
vulnérabilités, SBOM, signature d'images, déploiement Kubernetes et politiques
d'admission.

L'état d'avancement est suivi dans [PROGRESSION.md](PROGRESSION.md).

## Stack

| | |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.0 (starters modulaires) |
| Base de données | PostgreSQL 18 |
| Migrations | Flyway |
| Build | Maven (wrapper commité) |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers |

## Prérequis

- JDK 21
- Docker (nécessaire aux tests d'intégration)

## Démarrage

```bash
cp .env.exemple .env      # puis renseigne les valeurs
docker compose up -d
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Le profil `dev` ajoute un catalogue de produits de démonstration
(`src/main/resources/db/dev`), sans lequel aucune commande n'est créable :
le prix provient du catalogue, jamais de la requête client.

## Configuration

Aucun secret n'est versionné. `.env.exemple` liste les variables attendues ;
copie-le en `.env`, qui est ignoré par Git et par Docker.

| Variable | Rôle |
|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | conteneur PostgreSQL |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | datasource de l'application |

## Tests

```bash
./mvnw test
```

Les tests d'intégration démarrent un vrai PostgreSQL 18 via Testcontainers —
aucune installation préalable, mais Docker doit tourner.

Pour accélérer les exécutions locales, active la réutilisation du conteneur :

```bash
echo "testcontainers.reuse.enable=true" >> ~/.testcontainers.properties
```

Ce réglage vit dans ton répertoire personnel, jamais dans le dépôt : la CI
repart ainsi toujours d'une base vierge.

## Image Docker

```bash
docker build -t order-management-api:0.0.1 .
```

Build multi-stage : le JDK, Maven et les sources restent dans l'étape de
construction. L'image publiée (~123 Mo) ne contient qu'un JRE et le jar,
et s'exécute sous un utilisateur non privilégié (UID 10001).

## Endpoints

| Méthode | Chemin | |
|---|---|---|
| `POST` | `/api/v1/orders` | création — 201 + en-tête `Location` |
| `GET` | `/api/v1/orders/{id}` | consultation |
| `POST` | `/api/v1/orders/{id}/confirm` | confirmation |
| `POST` | `/api/v1/orders/{id}/cancel` | annulation |

Les changements d'état passent par `POST` et non `GET` : une lecture ne doit
jamais modifier d'état (CSRF, préchargement par le navigateur).

## Architecture

Organisation par fonctionnalité, avec une approche hexagonale pragmatique
appliquée au seul agrégat `order`.

```
com.example.devsecops
├── order
│   ├── domain        Order, OrderStatus, OrderLine   (aucun import Spring/JPA)
│   ├── web           contrôleur, DTO, mapper
│   ├── persistence   entités JPA, repositories, mapper
│   └── OrderService
└── shared/error      GlobalExceptionHandler
```

Le domaine ne connaît ni le web ni la persistance. Trois représentations
distinctes coexistent : DTO, objet métier, entité JPA.

## Choix notables

- **Identifiants UUID** partout, pour empêcher l'énumération des ressources
- **Le prix provient du catalogue**, jamais de la requête : le client n'envoie
  qu'un `productId` et une quantité
- **Prix figé dans la ligne de commande** : une évolution du catalogue ne
  réécrit pas une commande passée
- **Verrouillage optimiste** (`@Version`) sur les transitions d'état, contre
  deux confirmations concurrentes
- **Flyway plutôt que `ddl-auto`** : le schéma est auditable et versionné,
  Hibernate se contente de le valider au démarrage
- **Message détaillé dans les logs, générique dans la réponse HTTP**

## Licence

Apache-2.0
