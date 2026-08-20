package com.example.devsecops.order.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Catalogue. C'est la SEULE source du prix : le client n'envoie jamais
 * qu'un productId et une quantite.
 *
 * Pas de @OneToMany vers order_lines : une association bidirectionnelle
 * qu'on n'utilise jamais est de la dette, pas une fonctionnalite.
 */
@Entity
@Table(name = "products")
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ProductJpaEntity() {
        // requis par JPA
    }

    /**
     * L'application ne cree jamais de produit : le catalogue vient du SQL.
     * Ce constructeur existe pour que les tests puissent fabriquer un produit
     * lisiblement, sans reflexion ni bricolage.
     */
    public ProductJpaEntity(UUID id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductJpaEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return ProductJpaEntity.class.hashCode();
    }

    @Override
    public String toString() {
        return "ProductJpaEntity{id=" + id + ", name='" + name + "', price=" + price + "}";
    }
}
