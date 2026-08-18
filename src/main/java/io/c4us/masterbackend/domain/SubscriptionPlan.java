package io.c4us.masterbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "subscription_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String price;
    private String colorHex;
    private String iconKey;
    private Long priorite;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String features;

    // --- NOUVELLES COLONNES ---
    @Column(name = "sms_alerte")
    private Boolean smsAlerte;

    @Column(name = "stock_alerte")
    private Boolean stockAlerte;

    @Column(name = "nombre_business")
    private Integer nombreBusiness;

    private Double cout;

    @Column(name = "grace_periode")
    private Integer gracePeriode;

    @Column(name = "nombre_jour_souscription")
    private Integer nombreJourSouscription;

    private Boolean dashboard;

    @Column(name = "email_alerte")
    private Boolean emailAlerte;

    @Column(name = "loyalty_access")
    private Boolean loyaltyAccess;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "nombre_categorie_par_business")
    private Integer nombreCategorieParBusiness;

    @Column(name = "nombre_prod_par_business")
    private Integer nombreProdParBusiness;

     @Column(name = "ia_active")
    private Boolean iaActive;

    @Column(name = "mini_dashboard")
    private Boolean miniDashboard;
}