package com.innowise.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateItemDto {
    @NotBlank
    private String name;
    private BigDecimal price;
}
