package io.c4us.masterbackend.domain;

import java.time.LocalDateTime;
import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    // ✅ Utilisez le type Objet 'Boolean' pour forcer Lombok à générer 'getDeleted()'
    @Column(nullable = false)
    private Boolean deleted = false; 

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
        if (this.deleted == null) this.deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}