package com.example.devsecops.order;

import com.example.devsecops.order.domain.Order;
import com.example.devsecops.order.domain.OrderLine;
import com.example.devsecops.order.exception.OrderNotFoundException;
import com.example.devsecops.order.exception.UnknownProductException;
import com.example.devsecops.order.persistence.OrderEntityMapper;
import com.example.devsecops.order.persistence.OrderJpaEntity;
import com.example.devsecops.order.persistence.OrderJpaRepository;
import com.example.devsecops.order.persistence.ProductJpaEntity;
import com.example.devsecops.order.persistence.ProductJpaRepository;
import com.example.devsecops.order.web.CreateOrderRequest;
import com.example.devsecops.order.web.OrderLineRequest;
import com.example.devsecops.order.web.OrderResponse;
import com.example.devsecops.order.web.OrderWebMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderJpaRepository orderRepository;
    private final ProductJpaRepository productRepository;

    public OrderService(OrderJpaRepository orderRepository, ProductJpaRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Map<UUID, ProductJpaEntity> catalogue = loadCatalogue(request.items());

        List<OrderLine> lines = request.items().stream()
                .map(item -> toDomainLine(item, catalogue.get(item.productId())))
                .toList();

        Order order = new Order(request.userRef(), lines);

        OrderJpaEntity saved = orderRepository.save(OrderEntityMapper.toEntity(order, catalogue));

        // On a deja l'objet metier : on ne va chercher que l'identifiant genere.
        return OrderWebMapper.toResponse(order, saved.getId());
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return OrderWebMapper.toResponse(OrderEntityMapper.toDomain(loadOrder(id)));
    }

    @Transactional
    public OrderResponse confirm(UUID id) {
        return applyTransition(id, Order::confirm);
    }

    @Transactional
    public OrderResponse cancel(UUID id) {
        return applyTransition(id, Order::cancel);
    }

    /**
     * Le coeur de la correction.
     *
     * L'entite renvoyee par findById est MANAGEE : tant qu'on est dans la
     * transaction, Hibernate la surveille. Il suffit de reporter le champ qui
     * a change et le dirty checking emet l'UPDATE au flush.
     *
     * Surtout PAS de save(OrderEntityMapper.toEntity(order)) : ca fabrique une
     * entite neuve et detachee, donc created_at a null, les lignes detruites
     * par orphanRemoval puis recreees, et un UPDATE complet la ou un
     * "UPDATE orders SET status = ?" suffisait.
     */
    private OrderResponse applyTransition(UUID id, Consumer<Order> transition) {
        OrderJpaEntity entity = loadOrder(id);

        Order order = OrderEntityMapper.toDomain(entity);
        transition.accept(order);          // la regle metier reste dans le domaine

        entity.setStatus(order.getStatus());

        return OrderWebMapper.toResponse(order);
    }

    private OrderJpaEntity loadOrder(UUID id) {
        return orderRepository.findWithLinesById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order " + id + " does not exist"));
    }

    /**
     * Reponse a la question posee en revue : pas de findById() dans une boucle.
     *
     * On deduplique les productId (3 lignes du meme produit = 1 seul chargement)
     * et on tire tout en UNE requete "where id in (...)". C'est O(1) requete
     * quel que soit le nombre de lignes, la ou la boucle serait en O(n).
     */
    private Map<UUID, ProductJpaEntity> loadCatalogue(List<OrderLineRequest> items) {
        Set<UUID> requestedIds = items.stream()
                .map(OrderLineRequest::productId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, ProductJpaEntity> catalogue = productRepository.findAllById(requestedIds).stream()
                .collect(Collectors.toMap(ProductJpaEntity::getId, Function.identity()));

        if (catalogue.size() != requestedIds.size()) {
            Set<UUID> missing = new LinkedHashSet<>(requestedIds);
            missing.removeAll(catalogue.keySet());
            throw new UnknownProductException("Unknown product(s): " + missing);
        }
        return catalogue;
    }

    /**
     * Le nom et le prix viennent du catalogue, jamais de la requete.
     * C'est ici que la faille "le client fixe son prix" est fermee.
     * Le prix est fige dans la ligne : une hausse au catalogue demain
     * ne doit pas reecrire une commande d'hier.
     */
    private OrderLine toDomainLine(OrderLineRequest item, ProductJpaEntity product) {
        return new OrderLine(
                product.getId(),
                product.getName(),
                product.getPrice(),
                item.quantity());
    }
}
