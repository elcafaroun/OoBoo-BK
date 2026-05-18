package io.c4us.masterbackend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "ville_structure")
public class VilleStructure {

      @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Ajout de cette ligne
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private String id;
    private String nomVille;
    private boolean isActive=true;
    
    
}
