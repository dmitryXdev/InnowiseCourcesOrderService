package com.innowise.orderservice.dto;

import lombok.Data;

@Data
public class OrderItemDto {
    private Long id;
    private Long orderId;
    private Long itemId;
    private Integer quantity;
}
