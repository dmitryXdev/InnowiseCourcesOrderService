package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.PageResponseDto;
import com.innowise.orderservice.dto.ResponseDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.security.UserPrincipal;
import com.innowise.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @PreAuthorize("hasRole('ADMIN') || #dto.userId == authentication.principal.id")
    @PostMapping
    public ResponseEntity<ResponseDto> saveOrder(@RequestBody @Valid CreateOrderDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto> getOrderById(@PathVariable("id") Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getOrderById(id, principal));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<Page<OrderDto>> getAll(@RequestParam(name = "start", required = false) LocalDate start,
                                                 @RequestParam(name = "end", required = false) LocalDate end,
                                                 @RequestParam(name = "statuses", required = false) String[] statuses,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(defaultValue = "totalPrice", required = false) String sortBy) {
        return ResponseEntity.ok(orderService.getAll(start, end, statuses, page, size, sortBy));
    }

    @PreAuthorize("hasRole('ADMIN') || #userId == authentication.principal.id")
    @GetMapping
    public ResponseEntity<PageResponseDto> getAllByUserId(@RequestParam("userId") Long userId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(defaultValue = "totalPrice", required = false) String sortBy) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, page, size, sortBy));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto> updateOrderById(@PathVariable("id") Long id,
                                                       @RequestBody @Valid UpdateOrderDto dto,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.updateOrderById(id, dto, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderById(@PathVariable("id") Long id, @AuthenticationPrincipal UserPrincipal principal) {
        orderService.deleteOrderById(id, principal);
        return ResponseEntity.noContent().build();
    }
}
