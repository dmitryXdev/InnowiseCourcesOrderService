package com.innowise.orderservice.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(exclude = "createdAt")
public class OrderDto {
    private Long id;
    private Long userId;
    private String orderStatus;
    private BigDecimal totalPrice;
    private List<OrderItemDto> items;
    private Boolean deleted;
    private LocalDateTime createdAt;
}
