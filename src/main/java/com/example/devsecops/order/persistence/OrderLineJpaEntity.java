package com.example.devsecops.order.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_lines")
public class OrderLineJpaEntity {

    /**
     * Identifiant PROPRE a la ligne, genere par Hibernate.
     * Surtout pas l'id du produit : deux commandes du meme produit
     * entreraient en collision de cle primaire.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    /** Nom et prix FIGES au moment de la commande : le catalogue peut bouger. */
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    protected OrderLineJpaEntity() {
        // requis par JPA
    }

    public OrderLineJpaEntity(ProductJpaEntity product, String productName,
                              BigDecimal unitPrice, Integer quantity) {
        this.product = product;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public OrderJpaEntity getOrder() {
        return order;
    }

    /** Appele uniquement par OrderJpaEntity.addLine(). */
    void setOrder(OrderJpaEntity order) {
        this.order = order;
    }

    public ProductJpaEntity getProduct() {
        return product;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrderLineJpaEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return OrderLineJpaEntity.class.hashCode();
    }

    @Override
    public String toString() {
        // Ni order ni product : ce sont des associations LAZY.
        return "OrderLineJpaEntity{id=" + id + ", productName='" + productName
                + "', unitPrice=" + unitPrice + ", quantity=" + quantity + "}";
    }
}
