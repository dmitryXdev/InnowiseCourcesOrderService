package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dao.OrderRepository;
import com.innowise.orderservice.dto.CreateOrderDto;
import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.dto.PageResponseDto;
import com.innowise.orderservice.dto.ResponseDto;
import com.innowise.orderservice.dto.UpdateOrderDto;
import com.innowise.orderservice.dto.UserInfoDto;
import com.innowise.orderservice.exception.AccessDeniedException;
import com.innowise.orderservice.exception.BadIncomeDataException;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.feign.UserServiceClient;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.Item;
import com.innowise.orderservice.model.Order;
import com.innowise.orderservice.model.OrderItem;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.security.UserPrincipal;
import com.innowise.orderservice.security.UserRole;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.specifiaction.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    private static final String ORDER_NOT_FOUND_MESSAGE = "No order found";
    private static final String ITEM_NOT_FOUND_MESSAGE = "Item not found";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    private UserInfoDto getUserInfoById(Long id) {
        UserInfoDto userInfo = userServiceClient.getUserInfoById(id);

        if(userInfo == null || userInfo.getId() == null) {
            throw new BadIncomeDataException(USER_NOT_FOUND_MESSAGE);
        }
        return userInfo;
    }

    @Override
    @Transactional
    public ResponseDto createOrder(CreateOrderDto dto) {
        if(dto == null) {
            throw new BadIncomeDataException("No data present");
        }

        UserInfoDto userInfo = getUserInfoById(dto.getUserId());

        Order order = new Order();

        List<OrderItem> items = getOrderItems(dto.getItems(), order);

        order.setUserId(dto.getUserId());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setItems(items);
        order.setDeleted(false);
        order.calculateTotalPrice();

        orderRepository.save(order);

        return ResponseDto.builder()
                .order(orderMapper.toDto(order))
                .userInfo(userInfo)
                .build();
    }

    private List<OrderItem> getOrderItems(Map<Long, Integer> items, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Item temp = itemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND_MESSAGE));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setItem(temp);
            orderItem.setQuantity(entry.getValue());

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private Page<OrderDto> getBySpecification(Pageable pageable, Specification<Order> specification) {
        return orderRepository.findAll(specification, pageable).map(orderMapper::toDto);
    }

    @Override
    public PageResponseDto getOrdersByUserId(Long userId, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        if(userId == null) {
            throw new BadIncomeDataException("No required data present");
        }

        UserInfoDto userInfo = getUserInfoById(userId);

        Specification<Order> specification = Specification.where(OrderSpecification.hasUserId(userId));

        Page<OrderDto> response = getBySpecification(pageable, specification);

        if(response.isEmpty()) {
            throw new EntityNotFoundException(ORDER_NOT_FOUND_MESSAGE);
        }

        return PageResponseDto.builder()
                .page(response)
                .userInfo(userInfo)
                .build();
    }

    @Override
    @Transactional
    public ResponseDto updateOrderById(Long id, UpdateOrderDto dto, UserPrincipal principal) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        if(dto.getDeleted() == null && dto.getItems() == null && dto.getStatus() == null) {
            throw new BadIncomeDataException("No data presented");
        }

        UserInfoDto userinfo = getUserInfoById(order.getUserId());

        if(principal.getRole().equals(UserRole.ADMIN.toString())) {
            return ResponseDto.builder()
                    .userInfo(userinfo)
                    .order(updateOrder(dto, order))
                    .build();
        }
        else if(principal.getId().equals(order.getUserId())) {
            dto.setDeleted(null);
            dto.setStatus(null);
            return ResponseDto.builder()
                    .userInfo(userinfo)
                    .order(updateOrder(dto, order))
                    .build();
        }
        else {
            throw new AccessDeniedException(HttpStatus.FORBIDDEN.name());
        }
    }

    private OrderDto updateOrder(UpdateOrderDto dto, Order order) {
        if(dto.getDeleted() != null) {
            order.setDeleted(dto.getDeleted());
        }
        if(dto.getItems() != null) {
            List<OrderItem> items = getOrderItems(dto.getItems(), order);
            order.setItems(items);
            order.calculateTotalPrice();
        }
        if(dto.getStatus() != null) {
            order.setOrderStatus(OrderStatus.valueOf(dto.getStatus()));
        }

        return orderMapper.toDto(order);
    }
    @Override
    @Transactional
    public void deleteOrderById(Long id, UserPrincipal principal) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        if(!principal.getRole().equals(UserRole.ADMIN.toString()) && !principal.getId().equals(order.getUserId())) {
            throw new AccessDeniedException(HttpStatus.FORBIDDEN.name());
        }

        order.setDeleted(true);
    }

    @Override
    public ResponseDto getOrderById(Long id, UserPrincipal principal) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        UserInfoDto userInfo = getUserInfoById(order.getUserId());

        if(!principal.getRole().equals(UserRole.ADMIN.toString()) && !principal.getId().equals(order.getUserId())) {
            throw new AccessDeniedException(HttpStatus.FORBIDDEN.name());
        }

        return ResponseDto.builder()
                .userInfo(userInfo)
                .order(orderMapper.toDto(order))
                .build();
    }

    @Override
    public Page<OrderDto> getAll(LocalDate start, LocalDate end, String[] statuses, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        Specification<Order> specification = Specification.unrestricted();
        if(start != null && end != null) {
            specification = specification.and(OrderSpecification.hasCreationDateIsBetween(start, end));
        }
        if(statuses != null && statuses.length != 0) {
            for (String status : statuses) {
                specification = specification.and(OrderSpecification.hasStatus(OrderStatus.valueOf(status)));
            }
        }

        return getBySpecification(pageable, specification);
    }
}
