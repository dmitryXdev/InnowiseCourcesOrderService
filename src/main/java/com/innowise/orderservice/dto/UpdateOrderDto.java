package com.innowise.orderservice.dto;

import lombok.Data;

import java.util.HashMap;

@Data
public class UpdateOrderDto {
    private String status;
    private Boolean deleted;
    private HashMap<Long, Integer> items;
}
