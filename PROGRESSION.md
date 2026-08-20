# Order Management API — progression DevSecOps

> Mis à jour le 19/08/2026. Projet fil rouge : une API Spring Boot menée
> jusqu'à une chaîne DevSecOps complète.

## Vue d'ensemble

| Partie | Sujet | État |
|---|---|---|
| 1 | Architecture (hexagonal pragmatique) | ✅ terminée |
| 2 | PostgreSQL + Flyway | ✅ terminée |
| 3 | Tests unitaires (JUnit, Mockito, tranches Spring) | ✅ terminée |
| 4 | Testcontainers | ✅ terminée *(challenge B en attente)* |
| 5 | Docker | ✅ terminée |
| 6 | **GitHub Actions** | 🔜 **suivante** |
| 7 | Gitleaks | ⬜ |
| 8 | SonarQube | ⬜ |
| 9 | OWASP Dependency-Check | ⬜ |
| 10 | Trivy | ⬜ |
| 11 | SBOM | ⬜ |
| 12 | Cosign | ⬜ |
| 13 | Kubernetes | ⬜ |
| 14 | Kyverno | ⬜ |
| 15 | OWASP ZAP | ⬜ |
| 16 | Pipeline complet | ⬜ |

**5 parties sur 16 terminées.** Les parties 1 à 4 constituent la base
applicative ; les parties 5 à 16 construisent la chaîne DevSecOps par-dessus.

---

## Partie 2 — PostgreSQL + Flyway ✅

### Les 6 correctifs fonctionnels
1. `confirm()` / `cancel()` persistent — via le dirty checking sur l'entité
   managée, pas un `save()` d'entité détachée
2. `Order.reconstitute()` — l'identifiant et le statut réels sont restaurés
3. Mapper de lignes — id propre généré, `addLine()` maintient les deux côtés
   de l'association
4. `ProductJpaRepository` injecté — le prix vient du catalogue, jamais du client
5. `OrderNotFoundException` + handler 404
6. `@Transactional` sur toutes les méthodes du service

### Au-delà de la liste
- `@Version` + `V2__add_orders_version.sql` — verrouillage optimiste contre
  deux `confirm()` concurrents
- Validation réparée : `@NotEmpty` + `List<@Valid ...>` (`@Positive` sur une
  `List` était inopérant)
- `GlobalExceptionHandler` complet : 400, 404, 409 (transition), 409
  (concurrence), 500 générique
- `toString()` des entités purgés des associations LAZY
- `equals` / `hashCode` corrects sur les entités JPA
- Seed de développement isolé dans `db/dev`, hors des migrations de production
- En-tête `Location` sur le 201
- Métadonnées `pom.xml` remplies (impact SBOM, partie 11)

---

## Partie 3 — Tests ✅

```
domaine        36 tests   ~0,04 s
service         3 tests   ~0,11 s   (Mockito)
web             5 tests   ~1,09 s   (@WebMvcTest)
```

**Notions acquises**
- La règle de placement : *qui détient la vérité — mon code, ou l'infrastructure ?*
- La recette en 5 temps : phrase → trace du chemin → ARRANGE → ACT → ASSERT
- `@Mock` / `@InjectMocks` / `thenReturn` / `thenAnswer` / `thenThrow`
- Réponses par défaut d'un mock non configuré (`null`, liste vide,
  `Optional.empty()`, `0`, `false`)
- Strict stubs : une dictée inutilisée signale un test qui se contredit
- Tranches : `@WebMvcTest` charge les contrôleurs **et** les
  `@RestControllerAdvice`, jamais les services
- `@MockitoBean`, `MockMvc`, `jsonPath`

**Pièges rencontrés** — `Optional.empty()` ≠ `null` · `isEqualByComparingTo`
≠ `isEqualTo` sur `BigDecimal` · imports déplacés en Spring Boot 4 · un `int`
primitif ne distingue pas *absent* de *zéro* · jamais d'import `.shaded.`

---

## Partie 4 — Testcontainers ✅

```
avant optimisation   3,372 s
après                2,076 s      (−38 %)
```

Le gain réel n'est pas ce pourcentage : c'est que la **prochaine** classe
de test d'intégration coûtera presque zéro (même conteneur, contexte Spring
réutilisé).

**Notions acquises**
- Pourquoi pas H2 : deux jeux de migrations à maintenir, et le schéma testé
  cesse d'être le schéma déployé
- `@ServiceConnection` — Boot extrait URL/user/password du conteneur
- Conteneur singleton (bloc `static`, jamais arrêté, Ryuk nettoie) —
  fonctionne partout, apporte le cache de contexte Spring
- `reuse` — hors du dépôt par construction, donc désactivé en CI
- Empreinte des migrations injectée dans le label du conteneur : modifier un
  fichier SQL force un conteneur neuf
- `@Sql` — l'état initial est déclaré, jamais supposé
- Le piège `@Transactional` sur un test d'intégration : on relit sa propre
  modification en mémoire, exactement comme avec un mock
- Le flush d'Hibernate : `save()` n'écrit rien, l'INSERT part au commit

**Preuve acquise** : le bug n°1 de la partie 2 (`confirm()` ne persistait pas)
est désormais impossible à réintroduire sans que la CI échoue.

---

## Partie 5 — Docker ✅

```
Dockerfile naïf, une seule étape   ~800 Mo
multi-stage                         123 Mo      −85 %
contexte de build                  4,89 ko
utilisateur                        uid=10001, non-root
```

**Notions acquises**
- **Contexte de build** — `docker build .` envoie tout le dossier au démon
  avant de lire le Dockerfile. Docker ne lit pas `.gitignore`
- **`.dockerignore` en liste blanche** — on refuse tout, on réautorise
  explicitement. Même principe que les NetworkPolicy. La dernière règle qui
  correspond l'emporte
- **Couches** — pile de calques immuables. On masque, on n'efface jamais :
  un secret copié puis supprimé reste récupérable dans l'historique de l'image
- **Cache de couches** — ce qui change rarement se copie en premier.
  `pom.xml` + `dependency:go-offline` avant `src`
- **Multi-stage** — deux `FROM`, deux images. `COPY --from=builder` est le
  seul pont. Le JDK, Maven et les sources ne partent jamais en production
- **Choix de l'image runtime** — jre-alpine (musl) / distroless (pas de
  shell) / jre Ubuntu (glibc, plus gros). Chaque paquet en moins est une CVE
  en moins à traiter
- **Non-root avec UID numérique** — `runAsNonRoot: true` de Kubernetes ne sait
  pas résoudre un nom d'utilisateur : il lui faut un nombre
- **Forme exec vs forme shell** — en forme exec la JVM est le processus 1 et
  reçoit `SIGTERM`, donc l'arrêt est gracieux. En forme shell, le signal se
  perd et Kubernetes finit par un `SIGKILL`
- **`HEALTHCHECK`** — `--start-period` est indispensable sur une JVM.
  Ignoré par Kubernetes, utile pour Docker et Compose
- **Liveness ≠ readiness** — un health incluant la base en liveness transforme
  une panne de base en boucle de redémarrage sur tous les pods
- **Épinglage par digest** — un tag est mobile, un digest ne l'est pas.
  Contrepartie : plus de correctifs automatiques, d'où Renovate/Dependabot
- **Configuration hors de l'image** — sans `DB_URL`, le conteneur sort en
  `Exited (1)`. Fail-fast, et preuve que l'image ne contient aucun secret
- **Réseau Compose** — un service se joint par son nom, jamais par `localhost`.
  `depends_on: condition: service_healthy` attend la base réellement prête

## À traiter — par ordre d'urgence

### Bloquant
- [ ] **Aucun dépôt Git initialisé.** `git init` + premier commit.
      La partie 6 en dépend entièrement. Vérifier que `.env` n'entre jamais
      dans l'historique.
- [ ] `README.md` — instructions de démarrage (dépôt destiné à être public)

### Décisions en suspens
- [ ] Deux lignes du même produit dans une commande : contrainte unique sur
      `(order_id, product_id)`, ou agrégation des quantités côté service ?
- [ ] `IllegalArgumentException → 400` générique : pratique, mais transforme
      aussi les bugs internes en 400. Exception de domaine dédiée à la place ?
- [ ] `int quantity` ne distingue pas *absent* de *zéro*. Passer à
      `Integer` + `@NotNull` pour deux messages d'erreur distincts ?

### Reporté à une partie ultérieure
- [ ] Challenge B — test de concurrence prouvant le `@Version`
- [ ] Agent Mockito déclaré dans le `pom.xml` (partie 6)
- [ ] Séparation surefire / failsafe : `*Test` rapides, `*IT` lents (partie 6)
- [ ] `GenericContainer` et `Network` (partie 15, avec OWASP ZAP)
- [ ] Renommer `devsecops` → `order-management-api` (artifactId + packages)
- [ ] `TODO SECURITY` sur `GET /{id}` — BOLA / OWASP API1:2023, à traiter
      avec l'authentification

### Ménage
- [ ] Vider `_to_delete/`
- [ ] Supprimer les paquets vides `customer/` et `product/`
