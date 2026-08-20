package com.example.devsecops.order.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregat Order. Zero import Spring / JPA : c'est la regle.
 *
 * Deux facons d'obtenir un Order, et deux seulement :
 *  - new Order(userRef, items)          -> creation, statut force a DRAFT
 *  - Order.reconstitute(...)            -> relecture depuis la persistance
 *
 * Le constructeur canonique est prive pour que les invariants soient
 * verifies une seule fois, quel que soit le chemin d'entree.
 */
public class Order {

    private final UUID orderId;
    private final UUID userRef;
    private final List<OrderLine> items;
    private OrderStatus status;

    private Order(UUID orderId, UUID userRef, List<OrderLine> items, OrderStatus status) {
        this.userRef = Objects.requireNonNull(userRef, "userRef is required");
        this.status = Objects.requireNonNull(status, "status is required");

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one line");
        }
        this.orderId = orderId;
        this.items = List.copyOf(items);
    }

    /** Creation d'une nouvelle commande : pas encore d'identifiant, toujours DRAFT. */
    public Order(UUID userRef, List<OrderLine> items) {
        this(null, userRef, items, OrderStatus.DRAFT);
    }

    /**
     * Reconstitution depuis la base : l'identifiant et le statut reels sont
     * restaures. Sans cette porte d'entree, une commande SHIPPED rechargee
     * redeviendrait DRAFT et la machine a etats serait contournable.
     */
    public static Order reconstitute(UUID orderId, UUID userRef, List<OrderLine> items, OrderStatus status) {
        Objects.requireNonNull(orderId, "orderId is required when reconstituting an order");
        return new Order(orderId, userRef, items, status);
    }

    public BigDecimal totalAmount() {
        return items.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderTransitionException(
                    "Cannot move order " + orderId + " from " + status + " to " + target);
        }
        status = target;
    }

    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public void ship() {
        transitionTo(OrderStatus.SHIPPED);
    }

    public void deliver() {
        transitionTo(OrderStatus.DELIVERED);
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserRef() {
        return userRef;
    }

    public List<OrderLine> getItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
