package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nameCat", "codeStructure"})
})
public class Category implements Serializable {
    @Id
    @UuidGenerator
    @Column(name = "id", unique = true, updatable = false)
    private String id;

    private String categoryId; // Votre ID métier si nécessaire
    private String nameCat;
    private String description;
    private String codeStructure; // Clé de liaison avec la Structure
    private String categoryPhotoUrl;
    
    private boolean isActive = true;

    @Transient
    private long productCount;

    // --- CHAMPS SPECIFIQUES AU MODE OFFLINE ---

    @Column(updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    // Date pivot pour la synchronisation
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // Soft delete pour informer le mobile de la suppression
    private boolean deleted = false;

    // Gestion des conflits
    @Version
    private Long version;
}