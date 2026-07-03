package com.innowise.orderservice.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.orderservice.config.TestConfig;
import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.PageResponse;
import com.innowise.orderservice.dto.PageResponseDto;
import com.innowise.orderservice.dto.ResponseDto;
import com.innowise.orderservice.dto.TokenValidationResponseDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.dto.UserInfoDto;
import com.innowise.orderservice.model.Item;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.forbidden;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {
    private final WireMockServer userService = new WireMockServer(8081);
    private final WireMockServer authService = new WireMockServer(8082);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    private final List<Long> items = new ArrayList<>();

    private final String MOCK_JWT_TOKEN = "Bearer token";

    @BeforeEach
    void setUpItems() {
        userService.start();
        authService.start();

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
        userService.stop();
        authService.stop();
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

    private void setUpPositiveWireMockAnswer() {
        UserInfoDto dto = UserInfoDto.builder()
                .id(0L)
                .email("some@email.com")
                .name("Name")
                .surname("Surname")
                .build();

        TokenValidationResponseDto response = TokenValidationResponseDto.builder()
                .valid(true)
                .role("ADMIN")
                .userId(null)
                .build();

        userService.stubFor(WireMock.post(urlPathMatching("/users/[^/]+/info"))
                .willReturn(okJson(objectMapper.writeValueAsString(dto))));
        authService.stubFor(WireMock.post(urlEqualTo("/auth/validate"))
                .willReturn(okJson(objectMapper.writeValueAsString(response))));
    }

    private void setUpNegativeWireMockAnswer() {
        TokenValidationResponseDto response = TokenValidationResponseDto.builder()
                .valid(false)
                .role(null)
                .userId(null)
                .build();

        userService.stubFor(WireMock.post(urlPathMatching("/users/[^/]+/info"))
                .willReturn(serverError()));
        authService.stubFor(WireMock.post(urlEqualTo("/auth/validate"))
                .willReturn(okJson(objectMapper.writeValueAsString(response))));
    }

    @Test
    void saveOrder_mustThrowExceptionOnNotValidToken() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);

        setUpNegativeWireMockAnswer();

        mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveOrder_shouldSaveAndReturnOrderAndUserInfo() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);

        setUpPositiveWireMockAnswer();

        ResponseDto responseDto = objectMapper.readValue(mockMvc.perform(post("/orders")
                .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        assertNotNull(responseDto);
        assertNotNull(responseDto.getOrder().getId());
        assertNotNull(responseDto.getUserInfo().getId());

    }

    @Test
    void saveOrder_shouldThrowExceptionOnNullField() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);
        orderDto.setItems(null);
        setUpPositiveWireMockAnswer();

        mockMvc.perform(post("/orders")
                .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveOrder_shouldThrowExceptionOnServiceUnavailable() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);

        TokenValidationResponseDto response = TokenValidationResponseDto.builder()
                .valid(true)
                .role("ADMIN")
                .userId(null)
                .build();

        authService.stubFor(WireMock.post(urlEqualTo("/auth/validate"))
                .willReturn(okJson(objectMapper.writeValueAsString(response))));

        userService.stubFor(WireMock.post(urlPathMatching("/users/[^/]+/info"))
                        .willReturn(serviceUnavailable()));

        mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isBadGateway());
    }

    @Test
    void getOrderById_shouldReturnOrderAndUserInfoById() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);

        setUpPositiveWireMockAnswer();

        ResponseDto responseDto = objectMapper.readValue(mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        ResponseDto value = objectMapper.readValue(mockMvc.perform(get("/orders/" + responseDto.getOrder().getId().toString())
                .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        assertNotNull(responseDto);
        assertNotNull(responseDto.getUserInfo());
        assertEquals(responseDto.getOrder().getId(), value.getOrder().getId());
    }

    @Test
    void getOrderById_shouldThrowExceptionOnNotAccessibleResource() throws Exception {
        CreateOrderDto orderDto = generateCreateOrderDto(0L);

        setUpPositiveWireMockAnswer();

        ResponseDto responseDto = objectMapper.readValue(mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        setUpNegativeWireMockAnswer();

        mockMvc.perform(get("/orders/" + responseDto.getOrder().getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_shouldReturnAllOrdersByStatusAnfCreationDate() throws Exception {
        setUpPositiveWireMockAnswer();

        List<OrderDto> saved = fillDbWithOrdersAndReturnOrders(15);

        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, OrderDto.class);

        PageResponse<OrderDto> pageResponse = objectMapper.readValue(mockMvc.perform(get("/orders/search")
                        .param("status", "CREATED")
                        .param("start", LocalDate.now().minusMonths(1).toString())
                        .param("end", LocalDate.now().toString())
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), type);

        List<OrderDto> orders = pageResponse.getContent();

        assertTrue(saved.containsAll(orders));
    }

    @Test
    void getAllByUserId_shouldReturnAllByUserId() throws Exception {
        setUpPositiveWireMockAnswer();

        fillDbWithOrdersAndReturnOrders(15);

        PageResponseDto response = objectMapper.readValue(mockMvc.perform(get("/orders")
                .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
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
    void getAllByUserId_shouldThrowExceptionOnMissingOrder() throws Exception {
        setUpPositiveWireMockAnswer();

        mockMvc.perform(get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .param("userId", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllByUserId_shouldThrowExceptionOnMissingUserId() throws Exception {
        setUpPositiveWireMockAnswer();

        mockMvc.perform(get("/orders")
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .param("userId", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrderById_shouldUpdateAndReturnOrderAndUserInfoById() throws Exception {
        setUpPositiveWireMockAnswer();

        OrderDto orderDto = fillDbWithOrdersAndReturnOrders(1).get(0);

        UpdateOrderDto update = new UpdateOrderDto();
        update.setDeleted(true);
        update.setStatus(OrderStatus.DELIVERED.name());

        ResponseDto response = objectMapper.readValue(mockMvc.perform(put("/orders/" + orderDto.getId())
                .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), ResponseDto.class);

        assertNotNull(response);
        assertEquals(OrderStatus.DELIVERED.name(), response.getOrder().getOrderStatus());
        assertTrue(response.getOrder().getDeleted());
    }

    @Test
    void updateOrderById_shouldThrowExceptionOnAllFieldsMissing() throws Exception {
        setUpPositiveWireMockAnswer();

        OrderDto orderDto = fillDbWithOrdersAndReturnOrders(1).get(0);

        UpdateOrderDto update = new UpdateOrderDto();

        mockMvc.perform(put("/orders/" + orderDto.getId())
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrderById_shouldThrowExceptionOnNotAccessibleResource() throws Exception {
        setUpPositiveWireMockAnswer();

        OrderDto orderDto = fillDbWithOrdersAndReturnOrders(1).get(0);

        setUpNegativeWireMockAnswer();

        UpdateOrderDto update = new UpdateOrderDto();
        update.setDeleted(true);
        update.setStatus(OrderStatus.DELIVERED.name());

        mockMvc.perform(put("/orders/" + orderDto.getId())
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOrderById_shouldDeleteOrderById() throws Exception {
        setUpPositiveWireMockAnswer();

        OrderDto orderDto = fillDbWithOrdersAndReturnOrders(1).get(0);

        mockMvc.perform(delete("/orders/" + orderDto.getId())
                .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN))
                .andExpect(status().isNoContent());

        assertTrue(orderRepository.findById(orderDto.getId()).get().getDeleted());
    }

    @Test
    void deleteOrderById_shouldThrowExceptionOnNotAccessibleResource() throws Exception {
        setUpPositiveWireMockAnswer();

        OrderDto orderDto = fillDbWithOrdersAndReturnOrders(1).get(0);

        setUpNegativeWireMockAnswer();

        mockMvc.perform(delete("/orders/" + orderDto.getId())
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN))
                .andExpect(status().isForbidden());
    }
}
