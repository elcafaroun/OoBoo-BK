package io.c4us.masterbackend.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class AppUser {
    
    @Id
    @UuidGenerator
    @Column(name = "id", unique = true, updatable = false)
    private String id;

    private String userName;
    private String userEmail;
    private String userPhone;
    
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String codeUser;
    
    @Version
    private Long version; 
    
    // ✅ CORRECTION : Supprimer le préfixe 'is' pour avoir getFirstLogin() et setFirstLogin() standardisés
    @Column(name = "is_first_login", nullable = false)
    private Boolean firstLogin = true;
    
    // ✅ CORRECTION : Supprimer le préfixe 'is' pour avoir getActive() et setActive() proprement générés
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false)
    private Boolean deleted = false; 

    private String userPassword;
    private String confirmationToken;
    private LocalDateTime tokenExpiryDate;
    private String userProfile;

    // Relation Many-to-Many via la table d'association
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude 
    @EqualsAndHashCode.Exclude 
    private Set<UserStructure> structures = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.firstLogin == null) this.firstLogin = true;
        if (this.active == null) this.active = true;
        if (this.deleted == null) this.deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 💡 Helper pour ajouter proprement une structure à l'utilisateur
    public void addStructure(UserStructure userStructure) {
        this.structures.add(userStructure);
        userStructure.setUser(this);
    }

    // 💡 Helper pour supprimer proprement une structure de l'utilisateur
    public void removeStructure(UserStructure userStructure) {
        this.structures.remove(userStructure);
        userStructure.setUser(null);
    }
}