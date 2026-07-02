package com.innowise.orderservice.service;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dto.CreateItemDto;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.dto.UpdateItemDto;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.model.Item;
import com.innowise.orderservice.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
    @Mock
    private ItemRepository itemRepository;

    private ItemMapper itemMapper = Mappers.getMapper(ItemMapper.class);

    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemServiceImpl(itemRepository, itemMapper);
    }

    @Test
    void saveItem_shouldSaveAndReturnItem() {
        CreateItemDto createItemDto = new CreateItemDto();
        createItemDto.setPrice(new BigDecimal(11));
        createItemDto.setName("item");

        Item item = new Item();
        item.setId(0L);
        item.setPrice(createItemDto.getPrice());
        item.setName(createItemDto.getName());

        when(itemRepository.save(any())).thenReturn(item);

        ItemDto itemDto = itemService.saveItem(createItemDto);

        assertNotNull(itemDto);
        assertEquals(createItemDto.getPrice(), itemDto.getPrice());
        assertEquals(createItemDto.getName(), itemDto.getName());
    }

    @Test
    void updateItemById_shouldUpdateAndReturnItemById() {
        Item item = new Item();
        item.setId(0L);

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        UpdateItemDto updateItemDto = new UpdateItemDto();
        updateItemDto.setName("new item");
        updateItemDto.setPrice(new BigDecimal(111));

        ItemDto itemDto = itemService.updateItemById(0L, updateItemDto);

        assertNotNull(itemDto);
        assertEquals(updateItemDto.getName(), itemDto.getName());
        assertEquals(updateItemDto.getPrice(), itemDto.getPrice());
    }

    @Test
    void deleteItemById_shouldDeleteItemById() {
        Item item = new Item();
        item.setId(0L);

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        itemService.deleteItemById(0L);

        verify(itemRepository, times(1)).delete(any(Item.class));
    }

    @Test
    void getAll_shouldReturnPageOfItems() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Item item = new Item();
            item.setName("item" + i);
            item.setPrice(new BigDecimal(i));
            item.setId((long) i);

            items.add(item);
        }

        when(itemRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(items));

        Page<ItemDto> page = itemService.getAll(1, 10, "askfda");

        assertNotNull(page);
        assertEquals(items.size(), page.getContent().size());
        assertEquals(items.stream().map(itemMapper::toDto).toList(), page.getContent());
    }
}
