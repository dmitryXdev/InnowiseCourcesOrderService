package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dto.CreateItemDto;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.dto.UpdateItemDto;
import com.innowise.orderservice.exception.BadIncomeDataException;
import com.innowise.orderservice.exception.EntityNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.model.Item;
import com.innowise.orderservice.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    private static final String NO_DATA_PRESENT_MESSAGE = "No data present";
    private static final String ITEM_NOT_FOUND_MESSAGE = "Item not found";

    @Override
    public ItemDto saveItem(CreateItemDto dto) {
        if(dto == null) {
            throw new BadIncomeDataException(NO_DATA_PRESENT_MESSAGE);
        }

        Item item = new Item();
        item.setName(dto.getName());
        item.setPrice(dto.getPrice());

        itemRepository.save(item);

        return itemMapper.toDto(item);
    }

    @Override
    public ItemDto updateItemById(Long id, UpdateItemDto dto) {
        if(dto == null) {
            throw new BadIncomeDataException(NO_DATA_PRESENT_MESSAGE);
        }

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND_MESSAGE));

        if(dto.getName() != null) {
            item.setName(dto.getName());
        }
        if(dto.getPrice() != null) {
            item.setPrice(dto.getPrice());
        }

        return itemMapper.toDto(item);
    }

    @Override
    public void deleteItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND_MESSAGE));

        itemRepository.delete(item);
    }

    @Override
    public Page<ItemDto> getAll(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        return itemRepository.findAll(pageable).map(itemMapper::toDto);
    }

    @Override
    public ItemDto getById(Long id) {
        return itemMapper.toDto(itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND_MESSAGE)));
    }
}
