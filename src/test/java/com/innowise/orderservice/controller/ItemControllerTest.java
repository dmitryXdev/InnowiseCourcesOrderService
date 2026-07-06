package com.innowise.orderservice.controller;

import com.innowise.orderservice.config.TestConfig;
import com.innowise.orderservice.dao.ItemRepository;
import com.innowise.orderservice.dto.CreateItemDto;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.dto.PageResponse;
import com.innowise.orderservice.dto.TokenValidationRequestDto;
import com.innowise.orderservice.dto.TokenValidationResponseDto;
import com.innowise.orderservice.dto.UpdateItemDto;
import com.innowise.orderservice.feign.AuthServiceClient;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.model.Item;
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
import java.util.List;

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
class ItemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemMapper itemMapper;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    private final String MOCK_JWT_TOKEN = "Bearer token";

    @BeforeEach
    void setUpFeign() {
        TokenValidationResponseDto response = TokenValidationResponseDto.builder()
                .valid(true)
                .role("ADMIN")
                .userId(null)
                .build();

        when(authServiceClient.validate(any(TokenValidationRequestDto.class))).thenReturn(response);
    }

    @AfterEach
    void clearDb() {
        itemRepository.deleteAll();
    }

    private List<Item> fillDbWithItemsAndReturn(int amount) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Item item = new Item();
            item.setPrice(BigDecimal.valueOf(i + 1.01));
            item.setName("Item" + i);

            items.add(itemRepository.save(item));
        }
        return items;
    }

    @Test
    void saveItem_shouldSaveAndReturnItem() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto();
        createItemDto.setName("Item");
        createItemDto.setPrice(BigDecimal.ONE);

        ItemDto item = objectMapper.readValue(mockMvc.perform(post("/items")
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), ItemDto.class);

        assertNotNull(item);
        assertEquals(createItemDto.getName(), item.getName());
    }

    @Test
    void updateItem_shouldUpdateAndReturnItem() throws Exception {
        Item item = fillDbWithItemsAndReturn(1).get(0);

        UpdateItemDto update = new UpdateItemDto();
        update.setName(item.getName() + ": updated");

        ItemDto dto = objectMapper.readValue(mockMvc.perform(put("/items/" + item.getId())
                        .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), ItemDto.class);

        assertNotNull(dto);
        assertEquals(update.getName(), dto.getName());
    }

    @Test
    void deleteItemById_shouldDeleteItem() throws Exception {
        Item item = fillDbWithItemsAndReturn(1).get(0);

        mockMvc.perform(delete("/items/" + item.getId())
                .header(HttpHeaders.AUTHORIZATION, MOCK_JWT_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAll_shouldReturnAllItems() throws Exception {
        List<Item> items = fillDbWithItemsAndReturn(15);

        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(PageResponse.class, ItemDto.class);

        PageResponse<ItemDto> pageResponse = objectMapper.readValue(mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), javaType);

        List<ItemDto> converted = items.stream().map(itemMapper::toDto).toList();

        assertNotNull(pageResponse);
        assertEquals(10, pageResponse.getSize());
        assertTrue(converted.containsAll(pageResponse.getContent()));
    }

    @Test
    void getItemById_shouldReturnItemById() throws Exception {
        Item item = fillDbWithItemsAndReturn(1).get(0);

        ItemDto itemDto = objectMapper.readValue(mockMvc.perform(get("/items/" + item.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), ItemDto.class);

        assertNotNull(itemDto);
        assertEquals(item.getId(), itemDto.getId());
    }
}
