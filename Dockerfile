# ==============================================================
#  ETAPE 1 : builder
#  Contient le JDK, Maven, le cache .m2 et tes sources.
#  Cette image n'est JAMAIS publiee : elle sert a produire le jar.
# ==============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# --- ce qui change rarement -----------------------------------
# Cette couche n'a qu'une entree : pom.xml. Tant qu'il ne bouge
# pas, Docker la reutilise et ne retelecharge aucune dependance.
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# --- ce qui change a chaque commit ----------------------------
COPY src src
RUN ./mvnw -B package -DskipTests \
    && mv target/*.jar /app/app.jar

# ==============================================================
#  ETAPE 2 : runtime
#  Page blanche. Ne contient QUE ce qu'on y copie explicitement.
#  C'est cette image-ci qui part en production.
# ==============================================================
FROM eclipse-temurin:21-jre-alpine

# Labels OCI : ils relient l'image a son depot source.
# Trivy (partie 10) et le SBOM (partie 11) les exploitent.
LABEL org.opencontainers.image.title="order-management-api" \
      org.opencontainers.image.description="Order Management API - projet fil rouge DevSecOps" \
      org.opencontainers.image.source="https://github.com/CHANGE_ME/order-management-api"

# Utilisateur non privilegie.
# Sur Alpine c'est adduser/addgroup (BusyBox), pas useradd/groupadd.
# -S = compte "systeme" : pas de mot de passe, pas de home.
# Le GROUPE d'abord, puis l'utilisateur qu'on y rattache avec -G.
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app

WORKDIR /app

# Le SEUL pont entre les deux etapes : le jar, et rien d'autre.
# --chown evite un RUN chown supplementaire, donc une couche de moins.
COPY --from=builder --chown=app:app /app/app.jar app.jar

# APRES le COPY : sinon la copie se ferait en root et l'utilisateur
# app ne pourrait pas lire le fichier.
USER 10001:10001

# Purement documentaire : n'ouvre aucun port, mais les outils le lisent.
EXPOSE 8080

# Forme EXEC obligatoire : la JVM devient le processus 1 et recoit
# SIGTERM directement. En forme shell, /bin/sh serait le processus 1
# et ne transmettrait pas le signal -> pas d'arret gracieux.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1