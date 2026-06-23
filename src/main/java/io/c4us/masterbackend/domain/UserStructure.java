package io.c4us.masterbackend.domain;

import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_structures")
public class UserStructure {

    @Id
    @UuidGenerator
    @Column(name = "id", unique = true, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore 
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = false)
    @JsonIgnore 
    private Structure structure;

    @Column(name = "role_in_structure")
    private String roleInStructure;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ CORRECTION : Vérifiez que l'utilisateur n'est pas null avant d'accéder à l'ID
    @JsonProperty("userId")
    public String getUserId() {
        return (this.user != null) ? this.user.getId() : null;
    }

    // ✅ CORRECTION : Vérifiez que la structure n'est pas null
    @JsonProperty("structureId")
    public String getStructureId() {
        return (this.structure != null) ? this.structure.getIdStructure() : null;
    }

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    @Getter(AccessLevel.NONE) // On demande à Lombok de ne pas générer le getter par défaut
    @Column(nullable = false)
    private boolean deleted = false; 

    // On crée le getter manuellement pour être sûr qu'il s'appelle getDeleted()
    public boolean getDeleted() {
        return this.deleted;
    }

    @JsonProperty("deleted")
public boolean isDeleted() {
    return this.deleted;
}
}