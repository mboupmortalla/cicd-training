package com.example.devsecops.order.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLineTest {

    private static final UUID PRODUCT = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void le_total_de_ligne_est_le_prix_fois_la_quantite() {
        OrderLine line = new OrderLine(PRODUCT, "Clavier", new BigDecimal("89.90"), 3);

        assertThat(line.lineTotal()).isEqualByComparingTo(new BigDecimal("269.70"));
    }

    @Test
    void un_prix_a_zero_est_accepte() {
        // Un article offert est un cas metier legitime, pas une erreur.
        OrderLine line = new OrderLine(PRODUCT, "Goodie", BigDecimal.ZERO, 1);

        assertThat(line.lineTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void productId_est_obligatoire() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new OrderLine(null, "Clavier", BigDecimal.ONE, 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void un_nom_vide_ou_blanc_est_refuse(String name) {
        assertThatThrownBy(() -> new OrderLine(PRODUCT, name, BigDecimal.ONE, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productName");
    }

    @Test
    void un_nom_null_est_refuse() {
        assertThatThrownBy(() -> new OrderLine(PRODUCT, null, BigDecimal.ONE, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_prix_negatif_est_refuse() {
        assertThatThrownBy(() -> new OrderLine(PRODUCT, "Clavier", new BigDecimal("-1.00"), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitPrice");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void une_quantite_non_strictement_positive_est_refusee(int quantity) {
        assertThatThrownBy(() -> new OrderLine(PRODUCT, "Clavier", BigDecimal.ONE, quantity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }
}
