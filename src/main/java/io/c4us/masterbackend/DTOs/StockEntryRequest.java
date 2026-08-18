package io.c4us.masterbackend.DTOs;

import lombok.Data;

@Data
public class StockEntryRequest {
    private String productQrCode;
    private String codeStructure;
    private Double quantity; // Quantité à ajouter
}