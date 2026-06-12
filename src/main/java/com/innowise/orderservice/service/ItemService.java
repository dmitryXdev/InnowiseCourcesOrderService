package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.CreateItemDto;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.dto.UpdateItemDto;
import org.springframework.data.domain.Page;

public interface ItemService {
    ItemDto saveItem(CreateItemDto dto);
    ItemDto updateItemById(Long id, UpdateItemDto dto);
    void deleteItemById(Long id);
    Page<ItemDto> getAll(int page, int size, String sortBy);
    ItemDto getById(Long id);
}
