package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.PageResponseDto;
import com.innowise.orderservice.dto.ResponseDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.security.UserPrincipal;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface OrderService {
    ResponseDto createOrder(CreateOrderDto dto);
    ResponseDto getOrderById(Long id, UserPrincipal principal);
    Page<OrderDto> getAll(LocalDate start, LocalDate end, String status, int page, int size, String sortBy);
    PageResponseDto getOrdersByUserId(Long userId, int page, int size, String sortBy);
    ResponseDto updateOrderById(Long id, UpdateOrderDto dto, UserPrincipal principal);
    void deleteOrderById(Long id, UserPrincipal principal);
}
