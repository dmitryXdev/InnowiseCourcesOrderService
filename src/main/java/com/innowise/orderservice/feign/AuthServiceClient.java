package com.innowise.orderservice.feign;

import com.innowise.orderservice.config.FeignConfiguration;
import com.innowise.orderservice.dto.TokenValidationRequestDto;
import com.innowise.orderservice.dto.TokenValidationResponseDto;
import com.innowise.orderservice.feign.fallbackfactory.AuthServiceClientFallbackFactory;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service",
        url = "${services.auth.url}",
        configuration = FeignConfiguration.class,
        fallbackFactory = AuthServiceClientFallbackFactory.class)
@Validated
public interface AuthServiceClient {
    @PostMapping("/auth/validate")
    TokenValidationResponseDto validate(@RequestBody @Valid TokenValidationRequestDto dto);
}
