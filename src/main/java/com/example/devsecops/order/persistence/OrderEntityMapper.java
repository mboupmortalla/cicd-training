package com.example.devsecops.order.persistence;

import com.example.devsecops.order.domain.Order;
import com.example.devsecops.order.domain.OrderLine;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domaine <-> entites JPA. Aucune logique metier ici.
 */
public final class OrderEntityMapper {

    private OrderEntityMapper() {
    }

    /**
     * Utilise UNIQUEMENT a la creation. Pour une mise a jour on ne reconstruit
     * jamais une entite neuve : on modifie l'entite managee et le dirty
     * checking ecrit l'UPDATE.
     *
     * Le rattachement ligne -> commande se fait ici parce que c'est le seul
     * endroit qui tient les deux objets en main.
     */
    public static OrderJpaEntity toEntity(Order order, Map<UUID, ProductJpaEntity> productsById) {
        OrderJpaEntity entity = new OrderJpaEntity(order.getUserRef(), order.getStatus());

        for (OrderLine line : order.getItems()) {
            ProductJpaEntity product = productsById.get(line.productId());
            entity.addLine(new OrderLineJpaEntity(
                    product,
                    line.productName(),
                    line.unitPrice(),
                    line.quantity()));
        }
        return entity;
    }

    public static Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> items = entity.getLines().stream()
                .map(OrderEntityMapper::toDomain)
                .toList();

        return Order.reconstitute(
                entity.getId(),
                entity.getUserRef(),
                items,
                entity.getStatus());
    }

    private static OrderLine toDomain(OrderLineJpaEntity line) {
        // getProduct().getId() n'initialise pas le proxy LAZY :
        // Hibernate connait deja l'identifiant.
        return new OrderLine(
                line.getProduct().getId(),
                line.getProductName(),
                line.getUnitPrice(),
                line.getQuantity());
    }
}
