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

    // Identifiant/Nom du plan souscrit
    private String planStructure;
    private LocalDateTime startSub;
    private LocalDateTime endSub;

    private boolean isActive = true;
    private double cout;
    private Long priorite;

    // --- SNAPSHOT DES RÈGLES DU PLAN ---
    @Column(name = "sms_alerte")
    private Boolean smsAlerte;

    @Column(name = "stock_alerte")
    private Boolean stockAlerte;

    @Column(name = "email_alerte")
    private Boolean emailAlerte;

    @Column(name = "ia_active")
    private Boolean iaActive;

    @Column(name = "dashboard")
    private Boolean dashboard;

    @Column(name = "mini_dashboard")
    private Boolean miniDashboard;

    @Column(name = "nombre_users")
    private Boolean nombreUsers;

    @Column(name = "loyalty_access")
    private Boolean loyaltyAccess;

    @Column(name = "grace_periode")
    private Integer gracePeriode;

    @Column(name = "nombre_jour_souscription")
    private Integer nombreJourSouscription;

    @Column(name = "nombre_categorie_par_business")
    private Integer nombreCategorieParBusiness;

    @Column(name = "nombre_prod_par_business")
    private Integer nombreProdParBusiness;

    @Version
    private Long version;

    // --- OFFLINE & SYNCHRONISATION ---
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    private boolean deleted = false;

    @OneToMany(mappedBy = "structure", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserStructure> users = new HashSet<>();

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}