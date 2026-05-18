package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String createdUserId;
    private String codeStructure;
    private String planStructure;
    private LocalDateTime startSub;
    private LocalDateTime endSub;

    private boolean isActive = true;

    private double cout;

    private Long priorite;

       @Version
    private Long version; //

    // --- AJOUTS POUR LE MODE OFFLINE ---

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    // Permet de savoir quand synchroniser
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // Permet de synchroniser la suppression vers le mobile
    private boolean deleted = false;

    // Pour gérer les conflits de modification concurrentielle
    
}
