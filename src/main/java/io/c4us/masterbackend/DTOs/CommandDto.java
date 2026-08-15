package io.c4us.masterbackend.DTOs;

import java.util.List;
import lombok.Data;

@Data
public class CommandDto {    
    private String id; // 👈 AJOUTÉ : L'UUID généré par Flutter en mode offline
    private String customerName;
    private String paymentMethod;
    private String customerId;
    private String codeStructure;
    private List<CommandLineDTO> items;
    private Double totalAmount;
    private String userId;
private String userName;
}