package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Persistable;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "commands")
public class Command implements Serializable, Persistable<String> {

    @Id
    private String id; // L'ID généré par Flutter (ex: CMD-123456)

    private String customerName;
    private String customerNum;
    private String status = "PENDING";
    private Double totalAmount;
    private Double totalCredit = 0.0;
    private String codeStructure;
    private String paymentMethod;
    private String userId;
    private String userName;
    private LocalDateTime orderDate = LocalDateTime.now();
    private LocalDateTime lastUpdated = LocalDateTime.now();
    private boolean deleted = false;

    @Version
    private Long version = 0L; // Initialisé à 0 pour forcer l'INSERT

    @OneToMany(mappedBy = "command", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CommandLine> items = new ArrayList<>();

    // --- Logique Persistable pour les IDs manuels ---
    @Override
    public String getId() {
        return id;
    }

    @Override
    @Transient // Ne pas stocker ce booléen en base
    public boolean isNew() {
        return version == null || version == 0L;
    }

    public void addLigneCommande(CommandLine ligneCommande) {
        items.add(ligneCommande);
        ligneCommande.setCommand(this);
    }
}