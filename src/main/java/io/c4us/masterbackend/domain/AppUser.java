package io.c4us.masterbackend.domain;

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

    private LocalDateTime createdDate = LocalDateTime.now();

    // Ajout pour la synchronisation
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Pour la gestion des conflits

    private boolean isActive = true;
    private boolean deleted = false; // Pour notifier le mobile d'une suppression

    private String userPassword;
    private String confirmationToken;
    private LocalDateTime tokenExpiryDate;
    private String userProfile;
    private String codeStructure;

    @jakarta.persistence.PreUpdate
    @jakarta.persistence.PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}