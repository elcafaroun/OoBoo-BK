package io.c4us.masterbackend.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@Table(name = "customers")
public class Customer implements Serializable {
    @Id
    private String id;
    private String numCust;
    private String codePin;
    private String customerName;
    private String codeStructure;
        private LocalDateTime createdDate = LocalDateTime.now();
           @Version
    private Long version; //

   // private boolean isActive= true;

}
