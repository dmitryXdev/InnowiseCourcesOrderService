package com.innowise.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenValidationResponseDto {
    private boolean valid;
    private Long userId;
    @NotBlank
    @NotNull
    private String role;
}
