package com.innowise.orderservice.kafka.consumer;

import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.kafka.PaymentEvent;
import com.innowise.orderservice.model.Order;
import com.innowise.orderservice.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentConsumer {
    private final OrderRepository orderRepository;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    @Transactional
    public void handle(PaymentEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Entity not found"));

        if(event.getStatus().equals("SUCCESS")) {
            order.setOrderStatus(OrderStatus.PAID);
        }
        if(event.getStatus().equals("FAILED")) {
            order.setOrderStatus(OrderStatus.CANCELED);
        }
    }
}
