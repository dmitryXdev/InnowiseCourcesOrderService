package com.innowise.orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;

@Data
public class CreateOrderDto {
    @NotNull
    private Long userId;

    @NotNull
    @NotEmpty
    private HashMap<Long, Integer> items;
}
