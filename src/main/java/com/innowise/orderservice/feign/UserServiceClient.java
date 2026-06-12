package com.innowise.orderservice.feign;

import com.innowise.orderservice.config.FeignConfiguration;
import com.innowise.orderservice.dto.UserInfoDto;
import com.innowise.orderservice.feign.fallbackfactory.UserServiceClientFallbackFactory;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "user-service",
        url = "${services.user.url}",
        configuration = FeignConfiguration.class,
        fallbackFactory = UserServiceClientFallbackFactory.class)
@Validated
public interface UserServiceClient {
    @PostMapping("/users/{id}/info")
    @Valid
    UserInfoDto getUserInfoById(@PathVariable("id") Long id);
}
