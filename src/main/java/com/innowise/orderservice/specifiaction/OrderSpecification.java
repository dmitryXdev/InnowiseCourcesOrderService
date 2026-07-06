package com.innowise.orderservice.specifiaction;

import com.innowise.orderservice.model.Order;
import com.innowise.orderservice.model.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class OrderSpecification {
    private OrderSpecification() {
    }

    public static Specification<Order> hasUserId(Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), id);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("orderStatus"), status);
    }

    public static Specification<Order> hasCreationDateIsBetween(LocalDate start, LocalDate end) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("createdAt"), start, end);
    }
}
