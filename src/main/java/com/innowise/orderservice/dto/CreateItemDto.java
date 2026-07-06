package com.innowise.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateItemDto {
    @NotNull
    @NotBlank
    private String name;
    @NotNull
    private BigDecimal price;
}
