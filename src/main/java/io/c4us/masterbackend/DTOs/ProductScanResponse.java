package io.c4us.masterbackend.DTOs;

import io.c4us.masterbackend.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductScanResponse {
    private boolean exists;
    private String message;
    private Product product;
}