package com.example.devsecops.order;

import com.example.devsecops.order.exception.OrderNotFoundException;
import com.example.devsecops.order.exception.UnknownProductException;
import com.example.devsecops.order.persistence.OrderJpaEntity;
import com.example.devsecops.order.persistence.OrderJpaRepository;
import com.example.devsecops.order.persistence.ProductJpaEntity;
import com.example.devsecops.order.persistence.ProductJpaRepository;
import com.example.devsecops.order.web.CreateOrderRequest;
import com.example.devsecops.order.web.OrderLineRequest;
import com.example.devsecops.order.web.OrderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test du service AVEC des mocks : aucune base de donnees ne demarre.
 * Duree : quelques millisecondes.
 */
// (1) Active Mockito : c'est lui qui va creer les faux objets ci-dessous.
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // (2) @Mock = "fabrique-moi un FAUX repository".
    //     Il a les memes methodes que le vrai, mais elles ne font rien
    //     et renvoient null, tant qu'on ne leur a pas dicte leur reponse.
    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private ProductJpaRepository productRepository;

    // (3) @InjectMocks = "cree un VRAI OrderService, et passe-lui les
    //     deux faux repositories dans son constructeur".
    //     C'est le seul objet reel du test : c'est lui qu'on teste.
    @InjectMocks
    private OrderService service;

    private static final UUID USER = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CLAVIER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORDER = UUID.fromString("abcde067-a8d5-ffff-bbbb-111111111111");

    @Test
    void le_prix_vient_du_catalogue_et_jamais_de_la_requete() {

        // ---------- ARRANGE : on prepare la situation ----------

        // Le produit tel qu'il existe "en base" : 89.90 EUR.
        ProductJpaEntity clavier =
                new ProductJpaEntity(CLAVIER, "Clavier mecanique", new BigDecimal("89.90"));

        // (4) On DICTE sa reponse au faux repository :
        //     "quand on t'appellera findAllById avec n'importe quoi -> renvoie le clavier".
        //     any() = "n'importe quel argument, je m'en fiche".
        when(productRepository.findAllById(any())).thenReturn(List.of(clavier));

        // (5) Le faux save() renvoie simplement ce qu'on lui a donne.
        //     getArgument(0) = le 1er parametre recu par save().
        //     En vrai, Hibernate y ajouterait un identifiant genere ;
        //     ici il restera null, et ce n'est pas grave : on ne le teste pas.
        when(orderRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));

        // La requete du client : UN clavier x2. Remarque bien : AUCUN PRIX.
        CreateOrderRequest requete = new CreateOrderRequest(
                USER,
                List.of(new OrderLineRequest(CLAVIER, 2)));

        // ---------- ACT : on execute la methode testee ----------

        OrderResponse reponse = service.create(requete);

        // ---------- ASSERT : on verifie ----------

        assertThat(reponse.items()).hasSize(1);

        // Le nom et le prix ont ete pris dans le CATALOGUE, pas dans la requete.
        assertThat(reponse.items().get(0).productName()).isEqualTo("Clavier mecanique");
        assertThat(reponse.items().get(0).unitPrice())
                .isEqualByComparingTo(new BigDecimal("89.90"));

        // 2 x 89.90 = 179.80
        assertThat(reponse.totalAmount()).isEqualByComparingTo(new BigDecimal("179.80"));
    }

    @Test
    void refuse_to_order_an_unknown_product(){
    when(productRepository.findAllById(any())).thenReturn(List.of());

    CreateOrderRequest requete = new CreateOrderRequest(USER, List.of(new OrderLineRequest(CLAVIER, 5)));


    assertThatThrownBy(() -> service.create(requete)).isInstanceOf(UnknownProductException.class);

    verify(orderRepository,never()).save(any());
    }

    @Test
    void refuse_to_confirm_an_unknown_order(){
        when(orderRepository.findWithLinesById(ORDER)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirm(ORDER)).isInstanceOf(OrderNotFoundException.class);
        verify(orderRepository,never()).save(any());
    }
}
