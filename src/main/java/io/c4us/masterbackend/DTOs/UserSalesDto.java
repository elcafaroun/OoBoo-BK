package io.c4us.masterbackend.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSalesDto {
    private String userId;
    private String userName;
    private Double totalSalesAmount;
}