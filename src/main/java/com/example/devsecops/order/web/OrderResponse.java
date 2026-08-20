package com.example.devsecops.order.web;

import com.example.devsecops.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID userRef,
        OrderStatus status,
        // On renvoie les lignes : le client ne fixe plus les prix,
        // il doit donc pouvoir verifier ceux que le serveur a retenus.
        List<OrderLineResponse> items,
        BigDecimal totalAmount
) {
}
