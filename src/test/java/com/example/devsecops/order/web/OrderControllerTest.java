package com.example.devsecops.order.web;

import com.example.devsecops.order.OrderService;
import com.example.devsecops.order.domain.InvalidOrderTransitionException;
import com.example.devsecops.order.domain.OrderStatus;
import com.example.devsecops.order.exception.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de TRANCHE : Spring demarre la couche web et rien d'autre.
 * Pas de base, pas de service reel. On teste le CONTRAT HTTP.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    // MockMvc simule des requetes HTTP sans ouvrir de port reseau.
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean : remplace le vrai OrderService par un mock DANS le contexte
    // Spring. C'est l'equivalent de @Mock, mais pour un bean.
    // (@MockBean existe encore mais est deprecie depuis Spring Boot 3.4.)
    @MockitoBean
    private OrderService orderService;

    private static final UUID USER = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CLAVIER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String CORPS_VALIDE = """
            {
              "userRef": "aaaaaaaa-0000-0000-0000-000000000001",
              "items": [ { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 2 } ]
            }
            """;
    private static final String CORPS_WITHOUT_QUANTITY = """
            {
              "userRef": "aaaaaaaa-0000-0000-0000-000000000001",
              "quantity": 0
              "items": [ { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 2 } ]
            }
            """;

    private static final String CORPS_SANS_LIGNE = """
        {
          "userRef": "aaaaaaaa-0000-0000-0000-000000000001",
          "items": []
        }
        """;

    @Test
    void creer_une_commande_renvoie_201_avec_le_lien_vers_la_ressource() throws Exception {

        // ARRANGE : le service est mocke, on dicte ce qu'il renvoie.
        UUID orderId = UUID.randomUUID();
        OrderResponse attendu = new OrderResponse(
                orderId,
                USER,
                OrderStatus.DRAFT,
                List.of(new OrderLineResponse(CLAVIER, "Clavier mecanique",
                        new BigDecimal("89.90"), 2, new BigDecimal("179.80"))),
                new BigDecimal("179.80"));

        when(orderService.create(any())).thenReturn(attendu);

        // ACT + ASSERT : la requete HTTP et les verifications s'enchainent.
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_VALIDE))

                // 201 Created, et pas 200 : une ressource a ete creee.
                .andExpect(status().isCreated())

                // L'en-tete Location dit au client OU trouver ce qu'il vient de creer.
                .andExpect(header().string("Location", endsWith("/api/v1/orders/" + orderId)))

                // jsonPath navigue dans le JSON de reponse : $ = la racine.
                .andExpect(jsonPath("$.items[0].productName").value("Clavier mecanique"))
                .andExpect(jsonPath("$.totalAmount").value(179.80));
    }

    // ------------------------------------------------------------------
    // A TOI. Quatre tests, dans cet ordre :
    //
    //  1. items vide  -> 400        (corps : "items": [])
    //  2. quantity: 0 -> 400
    //  3. GET /api/v1/orders/{id} quand le service leve
    //     OrderNotFoundException  -> 404
    //  4. POST /{id}/confirm quand le service leve
    //     InvalidOrderTransitionException -> 409
    //
    // Indices :
    //  - pour 1 et 2, le service n'est JAMAIS appele : la validation @Valid
    //    rejette la requete avant d'entrer dans le controleur. Donc pas de
    //    when(...) du tout -- sinon Mockito rale (stub inutilise).
    //  - pour 3 et 4, on dicte au mock de LEVER une exception :
    //        when(orderService.findById(any())).thenThrow(new ...);
    //  - import statique a ajouter pour le GET : MockMvcRequestBuilders.get
    // ------------------------------------------------------------------

    @Test
    void send_order_bad_request_with_empty_items() throws Exception {
        mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_SANS_LIGNE)
        )
                .andExpect(status().isBadRequest());
        verify(orderService,never()).create(any());
    }

    @Test
    void send_order_not_found_with_unknown_order_id() throws Exception {
        when(orderService.findById(any())).thenThrow(new OrderNotFoundException("Order not found"));
        mockMvc.perform(
                get("/api/v1/orders/" + UUID.randomUUID())
        ).andExpect(status().isNotFound());

    }

    @Test
    void send_order_bad_request_with_a_null_quantity() throws Exception {
        mockMvc.perform(
                post("/api/v1/orders")
                        .content(CORPS_WITHOUT_QUANTITY)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest());

        verify(orderService,never()).create(any());
    }

    @Test
    void refuse_to_confirm_order_transition() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.confirm(orderId)).thenThrow(new InvalidOrderTransitionException("can't confirm order transition"));
        mockMvc.perform(
                post("/api/v1/orders/" + orderId+"/confirm")
        ).andExpect(status().is(409));
    }
}
