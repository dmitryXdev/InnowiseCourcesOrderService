package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderDto;
import com.innowise.orderservice.model.Order;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {ItemMapper.class, OrderItemMapper.class})
public interface OrderMapper {
    OrderDto toDto(Order order);

    @InheritInverseConfiguration
    Order toEntity(OrderDto orderDto);
}
