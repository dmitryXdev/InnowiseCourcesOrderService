package com.innowise.orderservice.kafka;

import com.innowise.orderservice.config.TestConfig;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.kafka.consumer.PaymentConsumer;
import com.innowise.orderservice.model.Order;
import com.innowise.orderservice.model.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfig.class)
class PaymentConsumerTest {
    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Autowired
    private PaymentConsumer paymentConsumer;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testKafkaListener() throws InterruptedException, ExecutionException {
        Order newOrder = new Order();
        newOrder.setTotalPrice(new BigDecimal(1));
        newOrder.setUserId(0L);
        newOrder.setDeleted(false);
        newOrder.setOrderStatus(OrderStatus.CREATED);


        newOrder = orderRepository.save(newOrder);

        final long id = newOrder.getId();

        PaymentEvent event = new PaymentEvent();
        event.setOrderId(newOrder.getId());
        event.setStatus("SUCCESS");

        kafkaTemplate.send("payment-events", event).get();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Order order = orderRepository.findById(id).get();

                    assertNotNull(order);
                    assertEquals(OrderStatus.PAID, order.getOrderStatus());
                });


    }
}
