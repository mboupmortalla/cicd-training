package com.example.devsecops.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Stream;

/**
 * Socle commun a TOUS les tests d'integration.
 *
 * Deux optimisations, et une seule idee derriere : ne payer le demarrage
 * de l'infrastructure qu'une seule fois.
 *
 * -------------------------------------------------------------------------
 * LEVIER 1 : conteneur singleton
 * -------------------------------------------------------------------------
 * Pas de @Testcontainers ni de @Container ici. Ces annotations font demarrer
 * ET ARRETER le conteneur autour de chaque CLASSE de test. Avec cinq classes,
 * c'est cinq demarrages de PostgreSQL.
 *
 * A la place : un champ static, demarre une fois dans le bloc static, et
 * JAMAIS arrete. Le nettoyage est assure par Ryuk, un conteneur sentinelle
 * que Testcontainers lance en parallele : il surveille la JVM et supprime
 * tout ce qu'elle a cree quand elle meurt -- meme apres un kill -9.
 *
 * Effet de bord, souvent le plus rentable : l'URL JDBC ne change plus d'une
 * classe a l'autre, donc la configuration Spring est identique, donc le
 * TestContext Framework REUTILISE le meme contexte applicatif au lieu d'en
 * reconstruire un. On economise le conteneur ET le demarrage de Spring.
 *
 * -------------------------------------------------------------------------
 * LEVIER 2 : reuse
 * -------------------------------------------------------------------------
 * withReuse(true) demande a Testcontainers de laisser le conteneur VIVANT
 * apres la fin du build, et de le reprendre au build suivant. Le demarrage
 * tombe alors a ~0.
 *
 * Ce drapeau ne suffit pas seul : il faut aussi, sur le poste du developpeur,
 * un fichier ~/.testcontainers.properties contenant
 *     testcontainers.reuse.enable=true
 *
 * C'est volontaire. Ce fichier n'existe pas sur un runner de CI, donc la
 * reutilisation y est automatiquement desactivee : la CI repart toujours
 * d'une base vierge. Rapide en local, reproductible en CI, sans branche
 * conditionnelle dans le code.
 *
 * -------------------------------------------------------------------------
 * La contrepartie du reuse
 * -------------------------------------------------------------------------
 * Le conteneur garde son etat entre deux builds. Un test qui supposerait une
 * base vide echouerait au deuxieme lancement. D'ou le @Sql ci-dessous, qui
 * remet l'etat initial AVANT CHAQUE test : la propriete "je pars d'un etat
 * connu" ne depend plus du hasard.
 *
 * A retenir aussi : si tu modifies une migration Flyway deja appliquee, le
 * conteneur reutilise gardera l'ancienne et Flyway refusera de demarrer
 * (checksum). C'est le rappel que les migrations appliquees sont immuables.
 * En cas de besoin : docker rm -f le conteneur.
 */
@SpringBootTest
@Sql("/db/reset-catalogue.sql")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine");

    static {
        // L'empreinte de reutilisation de Testcontainers est calculee sur la
        // CONFIGURATION du conteneur (image, env, ports, labels) -- jamais sur
        // le contenu de la base. On y injecte donc nous-memes une empreinte de
        // nos fichiers SQL : modifier une migration change le label, donc
        // change l'empreinte, donc force un conteneur neuf.
        POSTGRES.withLabel("app.migrations.fingerprint", migrationsFingerprint());
        POSTGRES.withReuse(true);
        POSTGRES.start();
    }

    /** SHA-256 de tous les fichiers sous src/main/resources/db, tronque. */
    private static String migrationsFingerprint() {
        Path racine = Path.of("src", "main", "resources", "db");
        try (Stream<Path> fichiers = Files.walk(racine)) {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            fichiers.filter(Files::isRegularFile)
                    .sorted()                       // ordre stable = empreinte stable
                    .forEach(fichier -> {
                        try {
                            sha.update(Files.readAllBytes(fichier));
                        } catch (Exception e) {
                            throw new UncheckedIOException(new java.io.IOException(fichier.toString(), e));
                        }
                    });
            return HexFormat.of().formatHex(sha.digest()).substring(0, 16);
        } catch (Exception e) {
            // En cas de doute, on ne reutilise rien : valeur unique.
            return "unknown-" + System.nanoTime();
        }
    }
}
