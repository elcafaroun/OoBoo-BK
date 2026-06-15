package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "structures")
public class Structure implements Serializable {

    @Id
    @UuidGenerator
    @Column(name = "id", unique = true, updatable = false)
    private String idStructure;

    private String nomStructure;
    private String phone1Structure;
    private String phone2Structure;
    private String paysStructure;
    private String villeStructure;
    private String rueStructure;
    private String codePoste;
    private String structPhotoUrl;
    private String emailStructure;
    private String typeStructure;
    private String disponibiliteStructure;
    private String geoLocStructure;
    private String descriptionStructure;
    private String codeStructure;
    private String planStructure;
    private LocalDateTime startSub;
    private LocalDateTime endSub;

    private boolean isActive = true;
    private double cout;
    private Long priorite;

    @Version
    private Long version; 

    // --- OFFLINE & SYNCHRONISATION ---
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    private boolean deleted = false;

    // Relation vers la table d'association
    @OneToMany(mappedBy = "structure", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserStructure> users = new HashSet<>();

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}