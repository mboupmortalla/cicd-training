package com.example.devsecops.order.web;

import com.example.devsecops.order.domain.Order;
import com.example.devsecops.order.domain.OrderLine;

import java.util.List;
import java.util.UUID;

/**
 * Un seul sens : domaine -> reponse.
 *
 * La construction du domaine a partir de la requete a quitte ce mapper :
 * elle a besoin du catalogue produits pour renseigner nom et prix, donc
 * d'un repository. Elle vit maintenant dans OrderService.
 */
public final class OrderWebMapper {

    private OrderWebMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        return toResponse(order, order.getOrderId());
    }

    /**
     * Variante utilisee juste apres un save() : l'identifiant vient d'etre
     * genere par la persistance, l'objet metier ne le connait pas encore.
     * Evite un aller-retour domaine -> entite -> domaine inutile.
     */
    public static OrderResponse toResponse(Order order, UUID orderId) {
        List<OrderLineResponse> items = order.getItems().stream()
                .map(OrderWebMapper::toResponse)
                .toList();

        return new OrderResponse(
                orderId,
                order.getUserRef(),
                order.getStatus(),
                items,
                order.totalAmount());
    }

    private static OrderLineResponse toResponse(OrderLine line) {
        return new OrderLineResponse(
                line.productId(),
                line.productName(),
                line.unitPrice(),
                line.quantity(),
                line.lineTotal());
    }
}
