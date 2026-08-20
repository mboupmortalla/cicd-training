package com.example.devsecops.order.persistence;

import com.example.devsecops.order.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID userRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Verrouillage optimiste. Deux POST /confirm concurrents lisent le meme
     * statut DRAFT et passent tous les deux le controle de transition ;
     * sans @Version, le second ecrase le premier en silence.
     * Avec @Version, il prend un OptimisticLockingFailureException -> 409.
     */
    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    protected OrderJpaEntity() {
        // requis par JPA
    }

    public OrderJpaEntity(UUID userRef, OrderStatus status) {
        this.userRef = userRef;
        this.status = status;
    }

    /**
     * Seul point d'entree pour ajouter une ligne : il maintient les DEUX cotes
     * de l'association. Sans le setOrder(this), order_id part a NULL et
     * la contrainte NOT NULL saute.
     */
    public void addLine(OrderLineJpaEntity line) {
        lines.add(line);
        line.setOrder(this);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserRef() {
        return userRef;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<OrderLineJpaEntity> getLines() {
        return lines;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrderJpaEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Constant : l'id est null avant le flush, un hash base dessus
        // changerait de valeur une fois l'entite dans un HashSet.
        return OrderJpaEntity.class.hashCode();
    }

    @Override
    public String toString() {
        // Jamais d'association LAZY ici : un simple log declencherait
        // une requete, ou une LazyInitializationException hors transaction.
        return "OrderJpaEntity{id=" + id + ", userRef=" + userRef
                + ", status=" + status + ", version=" + version + "}";
    }
}
