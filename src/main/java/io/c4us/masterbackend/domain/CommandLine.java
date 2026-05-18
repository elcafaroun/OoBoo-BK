package io.c4us.masterbackend.domain;

import java.io.Serializable;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Data
@Table(name = "command_lines")
public class CommandLine implements Serializable {

    @Id
    @UuidGenerator
    private String id;

    private String productId; // AJOUT : Indispensable pour mettre à jour le stock au retour d'internet
    private String productName;
    private int quantity;        
    private double unitPrice;     
    private String codeStructure;
     @Version
    private Long version; //

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id", nullable = false)
    @JsonBackReference
    private Command command;

    public double getSubTotal() {
        return quantity * unitPrice;
    }
}
