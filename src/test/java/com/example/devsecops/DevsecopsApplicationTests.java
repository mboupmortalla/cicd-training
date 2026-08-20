package com.example.devsecops;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Ce test genere par Spring Initializr demarre le contexte COMPLET :
 * il lui faut donc DB_URL/DB_USERNAME/DB_PASSWORD et un PostgreSQL joignable.
 * Sur une machine de CI vierge, il echoue - et il fait echouer tout le build.
 *
 * On le neutralise le temps de la Partie 3. Il redeviendra vert en Partie 4,
 * quand Testcontainers fournira une vraie base jetable au demarrage du test.
 *
 * Regle a retenir : un test qui a besoin d'une infrastructure externe non
 * fournie par le build n'est pas un test, c'est un piege pour le pipeline.
 */
class DevsecopsApplicationTests {

    @Test
    void contextLoads() {
    }

}
