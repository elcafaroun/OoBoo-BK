package io.c4us.masterbackend.DTOs;
import lombok.Data;

@Data
public class SettleCreditDto {
    private Double amountPaid;    // Montant saisi dans le dialogue Flutter
    private String paymentMethod; // Mode choisi (Espèces, Orange Money, etc.)
}