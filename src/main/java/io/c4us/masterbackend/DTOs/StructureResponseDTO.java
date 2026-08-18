package io.c4us.masterbackend.DTOs;

import lombok.*;
import java.time.LocalDateTime;

import jakarta.persistence.Column;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StructureResponseDTO {

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

    private boolean isActive;
    private double cout;
    private Long priorite;

    private Boolean iaActive;
    private Boolean miniDashboard;


    // --- SNAPSHOT DES RÈGLES DU PLAN ---
    private Boolean smsAlerte;
    private Boolean stockAlerte;
    private Boolean emailAlerte;
    private Boolean dashboard;
    private Boolean nombreUsers;
    private Boolean loyaltyAccess;
    private Integer gracePeriode;
    private Integer nombreJourSouscription;
    private Integer nombreCategorieParBusiness;
    private Integer nombreProdParBusiness;

    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;
}