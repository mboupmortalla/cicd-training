package com.example.devsecops.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du domaine : ZERO annotation Spring, zero mock, zero base.
 * Ils tournent en quelques millisecondes parce qu'il n'y a rien a demarrer.
 * C'est la couche ou l'on doit etre exhaustif : c'est la que vit le metier.
 */
class OrderTest {

    private static final UUID USER = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID KEYBOARD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MOUSE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static OrderLine keyboard(int quantity) {
        return new OrderLine(KEYBOARD, "Clavier mecanique", new BigDecimal("89.90"), quantity);
    }

    private static OrderLine mouse(int quantity) {
        return new OrderLine(MOUSE, "Souris ergonomique", new BigDecimal("45.00"), quantity);
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void une_nouvelle_commande_demarre_en_DRAFT_et_sans_identifiant() {
            Order order = new Order(USER, List.of(keyboard(1)));

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
            assertThat(order.getOrderId()).isNull();   // l'id vient de la persistance
            assertThat(order.getUserRef()).isEqualTo(USER);
        }

        @Test
        void userRef_est_obligatoire() {
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> new Order(null, List.of(keyboard(1))))
                    .withMessageContaining("userRef");
        }

        @Test
        void une_commande_vide_est_refusee() {
            assertThatThrownBy(() -> new Order(USER, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");
        }

        @Test
        void une_commande_sans_liste_est_refusee() {
            assertThatThrownBy(() -> new Order(USER, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /**
         * Copie DEFENSIVE : si l'appelant garde une reference sur sa liste et la
         * modifie ensuite, la commande ne doit pas bouger. Sans List.copyOf,
         * on peut ajouter une ligne a une commande deja livree.
         */
        @Test
        void la_liste_source_ne_peut_plus_influencer_la_commande() {
            List<OrderLine> source = new ArrayList<>(List.of(keyboard(1)));
            Order order = new Order(USER, source);

            source.add(mouse(3));

            assertThat(order.getItems()).hasSize(1);
        }

        @Test
        void les_lignes_exposees_sont_en_lecture_seule() {
            Order order = new Order(USER, List.of(keyboard(1)));

            assertThatThrownBy(() -> order.getItems().add(mouse(1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Montant")
    class Montant {

        @Test
        void le_total_est_la_somme_des_lignes() {
            Order order = new Order(USER, List.of(keyboard(2), mouse(1)));

            // 2 x 89.90 + 1 x 45.00 = 224.80
            //
            // isEqualByComparingTo et PAS isEqualTo : BigDecimal.equals compare
            // aussi le scale, donc 224.80 != 224.8. Se faire avoir la-dessus
            // est un rite de passage ; ne le subis qu'une fois.
            assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("224.80"));
        }

        @Test
        void une_commande_a_une_seule_ligne_vaut_cette_ligne() {
            Order order = new Order(USER, List.of(keyboard(1)));

            assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("89.90"));
        }
    }

    @Nested
    @DisplayName("Machine a etats")
    class MachineAEtats {

        @Test
        void une_commande_DRAFT_peut_etre_confirmee() {
            Order order = new Order(USER, List.of(keyboard(1)));

            order.confirm();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        void une_commande_DRAFT_peut_etre_annulee() {
            Order order = new Order(USER, List.of(keyboard(1)));

            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void confirmer_deux_fois_est_refuse() {
            Order order = new Order(USER, List.of(keyboard(1)));
            order.confirm();

            assertThatThrownBy(order::confirm)
                    .isInstanceOf(InvalidOrderTransitionException.class);
        }

        @Test
        void on_ne_peut_pas_expedier_une_commande_non_confirmee() {
            Order order = new Order(USER, List.of(keyboard(1)));

            assertThatThrownBy(order::ship)
                    .isInstanceOf(InvalidOrderTransitionException.class);
        }

        @Test
        void le_statut_ne_change_pas_quand_la_transition_est_refusee() {
            Order order = new Order(USER, List.of(keyboard(1)));

            assertThatThrownBy(order::deliver).isInstanceOf(InvalidOrderTransitionException.class);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        }

        @Test
        void le_cycle_de_vie_nominal_va_jusqu_a_DELIVERED() {
            Order order = new Order(USER, List.of(keyboard(1)));

            order.confirm();
            order.ship();
            order.deliver();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(order.getStatus().isFinal()).isTrue();
        }

        @ParameterizedTest(name = "{0} -> {1} : {2}")
        @CsvSource({
                "DRAFT,     CONFIRMED, true",
                "DRAFT,     CANCELLED, true",
                "DRAFT,     SHIPPED,   false",
                "DRAFT,     DELIVERED, false",
                "DRAFT,     DRAFT,     false",
                "CONFIRMED, SHIPPED,   true",
                "CONFIRMED, CANCELLED, true",
                "CONFIRMED, CONFIRMED, false",
                "CONFIRMED, DELIVERED, false",
                "SHIPPED,   DELIVERED, true",
                "SHIPPED,   CANCELLED, false",
                "DELIVERED, CANCELLED, false",
                "DELIVERED, SHIPPED,   false",
                "CANCELLED, CONFIRMED, false",
                "CANCELLED, DRAFT,     false"
        })
        void la_matrice_des_transitions_est_respectee(OrderStatus from, OrderStatus to, boolean allowed) {
            assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"})
        void un_etat_final_n_autorise_plus_aucune_transition(OrderStatus finalStatus) {
            assertThat(finalStatus.isFinal()).isTrue();

            for (OrderStatus target : OrderStatus.values()) {
                assertThat(finalStatus.canTransitionTo(target)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Reconstitution depuis la persistance")
    class Reconstitution {

        /**
         * LE test de non-regression du bug le plus grave de la Partie 2 :
         * le mapper repassait par le constructeur, qui force DRAFT.
         * Une commande SHIPPED rechargee redevenait DRAFT et la machine
         * a etats devenait contournable.
         */
        @Test
        void reconstitute_restaure_l_identifiant_et_le_statut_reels() {
            UUID id = UUID.randomUUID();

            Order order = Order.reconstitute(id, USER, List.of(keyboard(1)), OrderStatus.SHIPPED);

            assertThat(order.getOrderId()).isEqualTo(id);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        void une_commande_SHIPPED_rechargee_ne_peut_plus_etre_confirmee() {
            Order order = Order.reconstitute(
                    UUID.randomUUID(), USER, List.of(keyboard(1)), OrderStatus.SHIPPED);

            assertThatThrownBy(order::confirm)
                    .isInstanceOf(InvalidOrderTransitionException.class);
        }

        @Test
        void une_commande_SHIPPED_rechargee_peut_etre_livree() {
            Order order = Order.reconstitute(
                    UUID.randomUUID(), USER, List.of(keyboard(1)), OrderStatus.SHIPPED);

            order.deliver();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        void reconstituer_sans_identifiant_est_refuse() {
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> Order.reconstitute(
                            null, USER, List.of(keyboard(1)), OrderStatus.DRAFT))
                    .withMessageContaining("orderId");
        }

        @Test
        void reconstituer_sans_statut_est_refuse() {
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> Order.reconstitute(
                            UUID.randomUUID(), USER, List.of(keyboard(1)), null))
                    .withMessageContaining("status");
        }
    }
}
