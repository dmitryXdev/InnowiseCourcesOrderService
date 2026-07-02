package com.innowise.orderservice.service;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.PageResponseDto;
import com.innowise.orderservice.dto.ResponseDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.dto.UserInfoDto;
import com.innowise.orderservice.exception.AccessDeniedException;
import com.innowise.orderservice.feign.UserServiceClient;
import com.innowise.orderservice.mapper.OrderItemMapper;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.Item;
import com.innowise.orderservice.model.Order;
import com.innowise.orderservice.model.OrderItem;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.security.UserPrincipal;
import com.innowise.orderservice.security.UserRole;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Spy
    private OrderItemMapper orderItemMapper = Mappers.getMapper(OrderItemMapper.class);

    @InjectMocks
    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, itemRepository, orderMapper, userServiceClient);
    }

    private List<OrderItem> generateItems(int quantity, Order order) {
        List<OrderItem> items = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            OrderItem dto = new OrderItem();
            dto.setId((long)i);
            dto.setQuantity((int)(Math.random() * 5) + 1);

            Item item = new Item();
            item.setPrice(new BigDecimal(1));
            item.setId((long)i);
            item.setName("Item" + i);
            dto.setItem(item);

            items.add(dto);
        }

        return items;
    }

    private List<Order> generateOrders(int quantity, Long userId) {
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            Order order = new Order();
            order.setId((long)i);
            order.setOrderStatus(OrderStatus.values()[(int) (Math.random() * OrderStatus.values().length)]);
            order.setDeleted(false);
            order.setItems(generateItems(5, order));
            order.calculateTotalPrice();
            order.setUserId(userId);
            order.setCreatedAt(LocalDateTime.now().minusMonths((int)(Math.random()* (i + 1) + 1)));

            orders.add(order);
        }

        return orders;
    }

    @Test
    void createOrder_shouldCreateOrderAndReturnOrderDto() {
        CreateOrderDto createOrderDto = new CreateOrderDto();
        createOrderDto.setUserId(0L);
        createOrderDto.setItems(new HashMap<>(Map.of(1L, 12)));

        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setId(0L);

        when(userServiceClient.getUserInfoById(anyLong())).thenReturn(userInfoDto);

        Item item = new Item();
        item.setName("item");
        item.setId(1L);
        item.setPrice(new BigDecimal(1));

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        Order order = new Order();
        order.setItems(new ArrayList<>());
        order.setId(1L);

        when(orderRepository.save(any())).thenReturn(new Order());

        ResponseDto responseDto = orderService.createOrder(createOrderDto);

        assertNotNull(responseDto);
        assertNotNull(responseDto.getOrder());
        assertEquals(userInfoDto.getId(), responseDto.getUserInfo().getId());
    }

    @Test
    void getOrdersByUserId_shouldReturnOrderByUserId() {
        List<Order> orders = generateOrders(5, 0L);

        Page<Order> page = new PageImpl<>(orders);

        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setId(0L);
        userInfoDto.setName("User");

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userServiceClient.getUserInfoById(anyLong())).thenReturn(userInfoDto);

        PageResponseDto responseDto = orderService.getOrdersByUserId(0L, 1, 10, "sort");

        assertNotNull(responseDto);
        assertTrue(responseDto.getPage().getContent().stream().allMatch(orderDto -> orderDto.getUserId().equals(0L)));
        assertEquals(0L, (long) responseDto.getUserInfo().getId());
    }

    @Test
    void updateOrderById_shouldUpdateOrderById() {
        Order order = generateOrders(1, 0L).get(0);

        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setId(0L);

        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(userServiceClient.getUserInfoById(anyLong())).thenReturn(userInfoDto);

        UpdateOrderDto updateOrderDto = new UpdateOrderDto();
        updateOrderDto.setStatus(OrderStatus.DELIVERED.name());
        UserPrincipal userPrincipal = new UserPrincipal(0L, UserRole.ADMIN.name());

        ResponseDto responseDto = orderService.updateOrderById(order.getId(), updateOrderDto, userPrincipal);

        assertNotNull(responseDto);
        assertNotNull(responseDto.getOrder());
        assertNotNull(responseDto.getUserInfo());
        assertEquals(updateOrderDto.getStatus(), responseDto.getOrder().getOrderStatus());
        verify(orderRepository, times(1)).findById(anyLong());
    }

    @Test
    void deleteOrderById_shouldThrowExceptionIfNeitherOwnerNorAdmin() {
        Order order = generateOrders(1, 0L).get(0);

        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.deleteOrderById(order.getId(), new UserPrincipal(1L, UserRole.USER.name())));
    }

    @Test
    void deleteOrderById_shouldDeleteIfAdmin() {
        Order order = generateOrders(1, 0L).get(0);

        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        orderService.deleteOrderById(order.getId(), new UserPrincipal(1L, UserRole.ADMIN.name()));

        verify(orderRepository, times(1)).findById(anyLong());
    }

    @Test
    void getAll_shouldReturnAllOrders() {
        List<Order> orders = generateOrders(15, 0L);

        Page<Order> page = new PageImpl<>(orders);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<OrderDto> answer = orderService.getAll(LocalDate.now().minusMonths(2),
                LocalDate.now(),
                new String[]{OrderStatus.DELIVERED.name(), OrderStatus.CREATED.name()},
                0, 1, "field");

        assertNotNull(answer);
        assertNotNull(answer.getContent());
        assertEquals(orders.stream().map(orderMapper::toDto).toList(), answer.getContent());
    }
}
