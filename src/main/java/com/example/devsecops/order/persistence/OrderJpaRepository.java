package com.example.devsecops.order.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    /**
     * Charge la commande ET ses lignes en une seule requete.
     * Sans ca : 1 requete pour la commande + 1 pour les lignes (N+1 en germe).
     */
    @EntityGraph(attributePaths = "lines")
    Optional<OrderJpaEntity> findWithLinesById(UUID id);
}
