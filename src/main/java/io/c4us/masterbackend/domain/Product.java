package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "products",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_name_category", 
            columnNames = {"productName", "categoryId", "codeStructure"}
        )
    }
)
public class Product implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Ajout de cette ligne
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private String id;

    private String productId;
    private String productPhotoUrl;
    private String productName;
    private Double productPrice;
    private Double prixAchat;
    private String productDescription;
    private Double productQte;
    private Double stockAlert;
    private String codeStructure;
    private String categoryId;
    private boolean isFavoris;
    private boolean isActive = true;

    @Transient
    private Category categoryDetails;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime lastUpdated;

    private boolean deleted = false;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}