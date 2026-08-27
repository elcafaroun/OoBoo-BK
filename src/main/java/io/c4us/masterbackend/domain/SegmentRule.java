package io.c4us.masterbackend.domain;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "segment_rules")
public class SegmentRule implements Serializable {

    @Id
    private String id; // Ex: "VIP", "GOLD", "STANDARD"

    @Column(nullable = false, unique = true)
    private String segmentName; // Nom lisible du segment

    private Double conversionRate = 1000.0; // Montant en FCFA requis pour 1 point (ex: 1000 FCFA = 1 pt)

    private Integer pointsEarned = 1; // Nombre de points attribués par tranche de conversionRate

    private Double minAmountOrder = 0.0; // Seuil minimal d'achat pour cumuler des points

    private String codeStructure; // Permet de personnaliser les règles par structure/business
}