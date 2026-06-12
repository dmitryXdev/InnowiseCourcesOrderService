package com.innowise.orderservice.controller;

import com.innowise.orderservice.config.TestConfig;
import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.PageResponse;
import com.innowise.orderservice.dto.PageResponseDto;
import com.innowise.orderservice.dto.ResponseDto;
import com.innowise.orderservice.dto.TokenValidationRequestDto;
import com.innowise.orderservice.dto.TokenValidationResponseDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.dto.UserInfoDto;
import com.innowise.orderservice.feign.AuthServiceClient;
import com.innowise.orderservice.feign.UserServiceClient;
import com.innowise.orderservice.model.Item;
import com.innowise.orderservice.model.Order;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Import(TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderServiceImpl orderService;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    private final List<Long> items = new ArrayList<>();
    private final static String token = "Bearer token";

    @BeforeEach
    void setUpFeign() {
        TokenValidationResponseDto response = TokenValidationResponseDto.builder()
                .valid(true)
                .role("ADMIN")
                .userId(null)
                .build();

        UserInfoDto dto = UserInfoDto.builder()
                .id(0L)
                .email("some@email.com")
                .name("Name")
                .surname("Surname")
                .build();

        when(authServiceClient.validate(any(TokenValidationRequestDto.class))).thenReturn(response);
        when(userServiceClient.getUserInfoById(any(Long.class))).thenReturn(dto);
    }

    @BeforeEach
    void setUpItems() {
        for (int i = 0; i < 15; i++) {
            Item item = new Item();
            item.setPrice(BigDecimal.valueOf(i + 1.01));
            item.setName("Item" + i);

            item = itemRepository.save(item);
            items.add(item.getId());
        }
    }

    @AfterEach
    void clearDb() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    private CreateOrderDto generateCreateOrderDto(Long userId) {
        Random random = new Random();
        CreateOrderDto dto = new CreateOrderDto();
        dto.setItems(new HashMap<>());
        dto.setUserId(userId);
        for (int i = 0; i < random.nextInt(items.size()) + 1; i++) {
            dto.getItems().put(items.get(i), random.nextInt(10) + 1);
        }

        return dto;
    }

    private List<OrderDto> fillDbWithOrdersAndReturnOrders(int amount) {
        List<OrderDto> dtos = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            CreateOrderDto orderDto = generateCreateOrderDto((long) i);

            dtos.add(orderService.createOrder(orderDto).getOrder());
        }

        return dtos;
    }

    @Test
    void saveOrder_shouldSaveAndReturnOrderAndUserInfo() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);

        ResponseDto responseDto = objectMapper.readValue(mockMvc.perform(post("/orders")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        assertNotNull(responseDto);
        assertNotNull(responseDto.getOrder().getId());
        assertNotNull(responseDto.getUserInfo().getId());
    }

    @Test
    void getOrderById_shouldReturnOrderAndUserInfoById() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);

        ResponseDto responseDto = objectMapper.readValue(mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        ResponseDto value = objectMapper.readValue(mockMvc.perform(get("/orders/" + responseDto.getOrder().getId().toString())
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        assertNotNull(responseDto);
        assertNotNull(responseDto.getUserInfo());
        assertEquals(responseDto.getOrder().getId(), value.getOrder().getId());
    }

    @Test
    void getAll_shouldReturnAllOrdersByCreationDate() throws Exception {
        List<OrderDto> saved = fillDbWithOrdersAndReturnOrders(15);

        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, OrderDto.class);

        PageResponse<OrderDto> pageResponse = objectMapper.readValue(mockMvc.perform(get("/orders/search")
                .param("status", "CREATED")
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), type);

        List<OrderDto> orders = pageResponse.getContent();

        assertTrue(saved.containsAll(orders));
    }

    @Test
    void getAllByUserId_shouldReturnAllByUserId() throws Exception {
        fillDbWithOrdersAndReturnOrders(15);

        PageResponseDto response = objectMapper.readValue(mockMvc.perform(get("/orders")
                .header(HttpHeaders.AUTHORIZATION, token)
                .param("userId", "0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), PageResponseDto.class);

        assertNotNull(response);
        assertNotNull(response.getPage());
        assertNotNull(response.getUserInfo());

        List<OrderDto> orders = response.getPage().getContent();

        assertEquals(orders.size(), orders.stream().filter(orderDto -> orderDto.getUserId().equals(0L)).count());
    }

    @Test
    void updateOrderById_shouldUpdateAndReturnOrderAndUserInfoById() throws Exception {
        OrderDto orderDto = fillDbWithOrdersAndReturnOrders(1).get(0);

        UpdateOrderDto update = new UpdateOrderDto();
        update.setDeleted(true);
        update.setStatus(OrderStatus.DELIVERED.name());

        ResponseDto response = objectMapper.readValue(mockMvc.perform(put("/orders/" + orderDto.getId())
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        assertNotNull(response);
        assertEquals(OrderStatus.DELIVERED.name(), response.getOrder().getOrderStatus());
        assertTrue(response.getOrder().getDeleted());
    }

    @Test
    void deleteOrderById_shouldDeleteOrderById() throws Exception {
        OrderDto orderDto = fillDbWithOrdersAndReturnOrders(1).get(0);

        mockMvc.perform(delete("/orders/" + orderDto.getId())
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        List<Order> orders = orderRepository.findAll();

        assertTrue(orders.isEmpty());
    }
}
