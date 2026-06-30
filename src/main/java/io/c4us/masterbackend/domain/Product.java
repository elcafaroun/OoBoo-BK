package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        ),
        // ✅ Sécurité : Un code QR/barres doit être unique au sein d'une même structure
        @UniqueConstraint(
            name = "uk_product_qrcode_structure",
            columnNames = {"productQrCode", "codeStructure"}
        )
    }
)
public class Product implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private String id;

    private String productId;
    private String productPhotoUrl;
    private String productName;
    
@JsonProperty("productQrCode") // Indique explicitement comment mapper le JSON
@Column(name = "product_qr_code")
private String productQrCode;

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